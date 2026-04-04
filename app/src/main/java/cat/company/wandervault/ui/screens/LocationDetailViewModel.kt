package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.usecase.DeleteActivityUseCase
import cat.company.wandervault.domain.usecase.DeleteHotelUseCase
import cat.company.wandervault.domain.usecase.GetActivitiesForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetArrivalTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetDestinationByIdUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetDocumentByIdUseCase
import cat.company.wandervault.domain.usecase.GetHotelForDestinationUseCase
import cat.company.wandervault.domain.usecase.SaveActivityUseCase
import cat.company.wandervault.domain.usecase.SaveHotelUseCase
import cat.company.wandervault.domain.usecase.UpdateDestinationUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * ViewModel for the Location Detail screen.
 *
 * Each instance is scoped to a single destination ID via a Koin key. Hotel edits are tracked
 * in memory and auto-saved to the database after a short debounce; [onSave] can also be called
 * explicitly (e.g. on navigate-away) to flush any pending edits immediately.
 *
 * @param getDestinationById Use-case that fetches a single destination by ID.
 * @param getArrivalTransport Use-case that fetches the transport used to arrive at a destination.
 * @param getDestinationsForTrip Use-case that fetches all destinations for a trip, used to
 *   determine whether the destination is the first or last stop.
 * @param getHotelForDestination Use-case that fetches the hotel for a destination.
 * @param saveHotel Use-case that persists a hotel record.
 * @param deleteHotel Use-case that removes a hotel record.
 * @param updateDestination Use-case that persists changes to a destination (e.g. notes).
 * @param getDocumentById Use-case that resolves a [cat.company.wandervault.domain.model.TripDocument]
 *   by its ID; used to look up the name of the source document linked to a hotel record.
 * @param getActivitiesForDestination Use-case that fetches activities for a destination.
 * @param saveActivity Use-case that persists an activity record.
 * @param deleteActivity Use-case that removes an activity record.
 */
class LocationDetailViewModel(
    private val getDestinationById: GetDestinationByIdUseCase,
    private val getArrivalTransport: GetArrivalTransportForDestinationUseCase,
    private val getDestinationsForTrip: GetDestinationsForTripUseCase,
    private val getHotelForDestination: GetHotelForDestinationUseCase,
    private val saveHotel: SaveHotelUseCase,
    private val deleteHotel: DeleteHotelUseCase,
    private val updateDestination: UpdateDestinationUseCase,
    private val getDocumentById: GetDocumentByIdUseCase,
    private val getActivitiesForDestination: GetActivitiesForDestinationUseCase,
    private val saveActivity: SaveActivityUseCase,
    private val deleteActivity: DeleteActivityUseCase,
) : ViewModel() {

    private val _destinationId = MutableStateFlow<Int?>(null)

    /**
     * True when the user has made unsaved hotel edits. While true, incoming DB emissions do not
     * overwrite the hotel edit state. Reset to false after a save so the next emission refreshes
     * from the persisted record (picking up the real DB-assigned ID on first insert).
     */
    private var _hasUnsavedHotelEdits = false

    /**
     * True when the user has made unsaved notes edits. While true, incoming DB emissions do not
     * overwrite the notes edit state. Reset to false after a save.
     */
    private var _hasUnsavedNotesEdits = false

    /** The activity currently being created or edited. `null` means the form is closed. */
    private val _activityDraft = MutableStateFlow<ActivityEditState?>(null)

    private val _uiState = MutableStateFlow<LocationDetailUiState>(LocationDetailUiState.Loading)
    val uiState: StateFlow<LocationDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _destinationId
                .filterNotNull()
                .flatMapLatest { id ->
                    combine(
                        getDestinationById(id),
                        getArrivalTransport(id),
                        getHotelForDestination(id),
                        getActivitiesForDestination(id),
                    ) { destination, arrivalTransport, hotel, activities ->
                        Pair(Triple(destination, arrivalTransport, hotel), activities)
                    }.flatMapLatest { (triple, activities) ->
                        val (destination, arrivalTransport, hotel) = triple
                        if (destination == null) {
                            flowOf(LocationDetailUiState.Error)
                        } else {
                            combine(
                                getDestinationsForTrip(destination.tripId),
                                _activityDraft,
                            ) { tripDestinations, activityDraft ->
                                val firstPosition = tripDestinations.firstOrNull()?.position
                                val lastPosition = tripDestinations.lastOrNull()?.position
                                val isFirst = destination.position == firstPosition
                                val isLast = destination.position == lastPosition
                                val hotelEditState = if (_hasUnsavedHotelEdits &&
                                    _uiState.value is LocationDetailUiState.Success
                                ) {
                                    (_uiState.value as LocationDetailUiState.Success).hotelEditState
                                } else {
                                    val sourceDocName = hotel?.sourceDocumentId?.let { docId ->
                                        getDocumentById(docId).first()?.name
                                    }
                                    hotel?.toEditState(sourceDocName) ?: HotelEditState()
                                }
                                val notes = if (_hasUnsavedNotesEdits &&
                                    _uiState.value is LocationDetailUiState.Success
                                ) {
                                    (_uiState.value as LocationDetailUiState.Success).notes
                                } else {
                                    destination.notes ?: ""
                                }
                                LocationDetailUiState.Success(
                                    destination = destination,
                                    arrivalTransport = arrivalTransport,
                                    isFirst = isFirst,
                                    isLast = isLast,
                                    hotelEditState = hotelEditState,
                                    notes = notes,
                                    activities = activities,
                                    activityDraft = activityDraft,
                                )
                            }
                        }
                    }
                }
                .collect { state -> _uiState.value = state }
        }

        // Auto-save: persist hotel and notes changes after a short debounce once editing stops.
        // Uses collect (not collectLatest) so that a DB-triggered _uiState emission cannot cancel
        // an in-flight persistHotel() or persistNotes() call after the unsaved-edits flag is
        // already cleared.
        viewModelScope.launch {
            _uiState
                .debounce(AUTO_SAVE_DEBOUNCE_MS)
                .collect {
                    persistHotel()
                    persistNotes()
                }
        }
    }

    /** Switch the screen to display the destination with the given [id]. */
    fun loadDestination(id: Int) {
        if (_destinationId.value != id) {
            _uiState.value = LocationDetailUiState.Loading
            _hasUnsavedHotelEdits = false
            _hasUnsavedNotesEdits = false
            _activityDraft.value = null
            _destinationId.value = id
        }
    }

    /** Updates the hotel name field. */
    fun onHotelNameChange(value: String) {
        _hasUnsavedHotelEdits = true
        updateHotelEditState { copy(name = value) }
    }

    /** Updates the hotel address field. */
    fun onHotelAddressChange(value: String) {
        _hasUnsavedHotelEdits = true
        updateHotelEditState { copy(address = value) }
    }

    /** Updates the hotel reservation number field. */
    fun onHotelReservationNumberChange(value: String) {
        _hasUnsavedHotelEdits = true
        updateHotelEditState { copy(reservationNumber = value) }
    }

    /** Updates the notes field. */
    fun onNotesChange(value: String) {
        _hasUnsavedNotesEdits = true
        val current = _uiState.value as? LocationDetailUiState.Success ?: return
        _uiState.value = current.copy(notes = value)
    }

    /** Flushes any pending hotel edits immediately (e.g. on navigate-away). */
    fun onSave() {
        viewModelScope.launch {
            persistHotel()
            persistNotes()
        }
    }

    /**
     * Removes the source document link from the hotel record.
     * The change will be persisted via the auto-save debounce.
     */
    fun onClearHotelSourceDocument() {
        val current = _uiState.value as? LocationDetailUiState.Success ?: return
        if (current.hotelEditState.sourceDocumentId == null) return
        _hasUnsavedHotelEdits = true
        updateHotelEditState { copy(sourceDocumentId = null, sourceDocumentName = null) }
    }

    // ── Activity draft management ─────────────────────────────────────────────

    /** Opens a blank activity draft form for creating a new activity. */
    fun onOpenNewActivityDraft() {
        _activityDraft.value = ActivityEditState()
    }

    /**
     * Opens the activity draft form pre-populated with an existing [activity] for editing.
     */
    fun onEditActivity(activity: Activity) {
        _activityDraft.value = ActivityEditState(
            id = activity.id,
            title = activity.title,
            description = activity.description,
            dateTime = activity.dateTime,
            confirmationNumber = activity.confirmationNumber,
        )
    }

    /** Closes the activity draft form without saving. */
    fun onCloseActivityDraft() {
        _activityDraft.value = null
    }

    /** Updates the title field of the activity draft. */
    fun onActivityDraftTitleChange(value: String) {
        _activityDraft.value = _activityDraft.value?.copy(title = value)
    }

    /** Updates the description field of the activity draft. */
    fun onActivityDraftDescriptionChange(value: String) {
        _activityDraft.value = _activityDraft.value?.copy(description = value)
    }

    /** Updates the date/time field of the activity draft. */
    fun onActivityDraftDateTimeChange(value: ZonedDateTime?) {
        _activityDraft.value = _activityDraft.value?.copy(dateTime = value)
    }

    /** Updates the confirmation number field of the activity draft. */
    fun onActivityDraftConfirmationNumberChange(value: String) {
        _activityDraft.value = _activityDraft.value?.copy(confirmationNumber = value)
    }

    /** Saves the current activity draft to the database and closes the form. */
    fun onSaveActivityDraft() {
        val draft = _activityDraft.value ?: return
        if (draft.title.isBlank()) return
        val destinationId = (_uiState.value as? LocationDetailUiState.Success)?.destination?.id ?: return
        viewModelScope.launch {
            saveActivity(
                Activity(
                    id = draft.id,
                    destinationId = destinationId,
                    title = draft.title.trim(),
                    description = draft.description.trim(),
                    dateTime = draft.dateTime,
                    confirmationNumber = draft.confirmationNumber.trim(),
                ),
            )
            _activityDraft.value = null
        }
    }

    /** Deletes the given [activity] from the database. */
    fun onDeleteActivity(activity: Activity) {
        viewModelScope.launch {
            deleteActivity(activity)
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private suspend fun persistHotel() {
        if (!_hasUnsavedHotelEdits) return
        val state = _uiState.value as? LocationDetailUiState.Success ?: return
        if (state.isFirst || state.isLast) {
            _hasUnsavedHotelEdits = false
            return
        }
        val destinationId = state.destination.id
        val edit = state.hotelEditState
        _hasUnsavedHotelEdits = false

        val hasContent = edit.name.isNotBlank() || edit.address.isNotBlank() || edit.reservationNumber.isNotBlank()
        if (hasContent) {
            saveHotel(
                Hotel(
                    id = edit.id,
                    destinationId = destinationId,
                    name = edit.name.trim(),
                    address = edit.address.trim(),
                    reservationNumber = edit.reservationNumber.trim(),
                    sourceDocumentId = edit.sourceDocumentId,
                ),
            )
        } else if (edit.id > 0) {
            // All fields cleared – delete the persisted hotel row so clearing is reflected in DB.
            deleteHotel(Hotel(id = edit.id, destinationId = destinationId))
        }
    }

    private inline fun updateHotelEditState(update: HotelEditState.() -> HotelEditState) {
        val current = _uiState.value as? LocationDetailUiState.Success ?: return
        _uiState.value = current.copy(hotelEditState = current.hotelEditState.update())
    }

    private suspend fun persistNotes() {
        if (!_hasUnsavedNotesEdits) return
        val state = _uiState.value as? LocationDetailUiState.Success ?: return
        _hasUnsavedNotesEdits = false
        val updatedNotes = state.notes.trim().takeIf { it.isNotEmpty() }
        updateDestination(state.destination.copy(notes = updatedNotes))
    }

    companion object {
        private const val AUTO_SAVE_DEBOUNCE_MS = 300L
    }
}

private fun Hotel.toEditState(sourceDocumentName: String? = null) = HotelEditState(
    id = id,
    name = name,
    address = address,
    reservationNumber = reservationNumber,
    sourceDocumentId = sourceDocumentId,
    sourceDocumentName = sourceDocumentName,
)

