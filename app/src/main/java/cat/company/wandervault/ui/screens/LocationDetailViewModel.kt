package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.usecase.DeleteHotelUseCase
import cat.company.wandervault.domain.usecase.GetArrivalTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetDestinationByIdUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.GetHotelForDestinationUseCase
import cat.company.wandervault.domain.usecase.SaveHotelUseCase
import cat.company.wandervault.domain.usecase.SummarizeDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateDestinationUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
 * @param summarizeDocument Use-case that runs ML Kit document extraction for reservation scan.
 */
class LocationDetailViewModel(
    private val getDestinationById: GetDestinationByIdUseCase,
    private val getArrivalTransport: GetArrivalTransportForDestinationUseCase,
    private val getDestinationsForTrip: GetDestinationsForTripUseCase,
    private val getHotelForDestination: GetHotelForDestinationUseCase,
    private val saveHotel: SaveHotelUseCase,
    private val deleteHotel: DeleteHotelUseCase,
    private val updateDestination: UpdateDestinationUseCase,
    private val summarizeDocument: SummarizeDocumentUseCase,
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

    /** The coroutine running the current document scan, kept so it can be cancelled. */
    private var _scanJob: Job? = null

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
                    ) { destination, arrivalTransport, hotel ->
                        Triple(destination, arrivalTransport, hotel)
                    }.flatMapLatest { (destination, arrivalTransport, hotel) ->
                        if (destination == null) {
                            flowOf(LocationDetailUiState.Error)
                        } else {
                            getDestinationsForTrip(destination.tripId).map { tripDestinations ->
                                val firstPosition = tripDestinations.firstOrNull()?.position
                                val lastPosition = tripDestinations.lastOrNull()?.position
                                val isFirst = destination.position == firstPosition
                                val isLast = destination.position == lastPosition
                                val hotelEditState = if (_hasUnsavedHotelEdits &&
                                    _uiState.value is LocationDetailUiState.Success
                                ) {
                                    (_uiState.value as LocationDetailUiState.Success).hotelEditState
                                } else {
                                    hotel?.toEditState() ?: HotelEditState()
                                }
                                val notes = if (_hasUnsavedNotesEdits &&
                                    _uiState.value is LocationDetailUiState.Success
                                ) {
                                    (_uiState.value as LocationDetailUiState.Success).notes
                                } else {
                                    destination.notes ?: ""
                                }
                                // Preserve any active scan dialog across DB emissions.
                                val currentScanState =
                                    (_uiState.value as? LocationDetailUiState.Success)?.scanState
                                LocationDetailUiState.Success(
                                    destination = destination,
                                    arrivalTransport = arrivalTransport,
                                    isFirst = isFirst,
                                    isLast = isLast,
                                    hotelEditState = hotelEditState,
                                    notes = notes,
                                    scanState = currentScanState,
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

    /**
     * Starts a document scan for hotel-reservation auto-fill.
     *
     * Sets [DocumentScanUiState.Loading] immediately, then runs [SummarizeDocumentUseCase] on the
     * file at [fileUri]. Transitions through [DocumentScanUiState.Downloading] if the on-device AI
     * model needs to be downloaded, and finally to [DocumentScanUiState.Result],
     * [DocumentScanUiState.Unavailable], or [DocumentScanUiState.Error].
     *
     * Any previously running scan is cancelled before starting a new one.
     *
     * @param fileUri URI of the document to scan (image or PDF).
     * @param mimeType MIME type of the document.
     */
    fun onScanDocument(fileUri: String, mimeType: String) {
        _scanJob?.cancel()
        updateScanState(DocumentScanUiState.Loading)
        _scanJob = viewModelScope.launch {
            val result = try {
                summarizeDocument(fileUri, mimeType) { bytes ->
                    updateScanState(DocumentScanUiState.Downloading(bytes))
                }
            } catch (e: Exception) {
                updateScanState(DocumentScanUiState.Error(e.message))
                return@launch
            }
            if (result == null) {
                updateScanState(DocumentScanUiState.Unavailable)
            } else {
                updateScanState(DocumentScanUiState.Result(result))
            }
        }
    }

    /**
     * Applies the extracted hotel info from the current [DocumentScanUiState.Result] to the
     * hotel edit state, then dismisses the scan dialog.
     *
     * Only blank fields in the current hotel edit state are filled in; existing non-blank values
     * are preserved so that user-entered data is never overwritten.
     *
     * No-op when the current scan state is not [DocumentScanUiState.Result] or the result
     * contains no [cat.company.wandervault.domain.model.HotelInfo].
     */
    fun onApplyScanResult() {
        val state = _uiState.value as? LocationDetailUiState.Success ?: return
        val result = (state.scanState as? DocumentScanUiState.Result)?.extractionResult ?: return
        val hotelInfo = result.hotelInfo ?: run {
            dismissScan()
            return
        }
        _hasUnsavedHotelEdits = true
        updateHotelEditState {
            copy(
                name = name.ifBlank { hotelInfo.name.orEmpty() },
                address = address.ifBlank { hotelInfo.address.orEmpty() },
                reservationNumber = reservationNumber.ifBlank { hotelInfo.bookingReference.orEmpty() },
            )
        }
        dismissScan()
    }

    /** Cancels any in-progress scan and hides the scan dialog. */
    fun dismissScan() {
        _scanJob?.cancel()
        _scanJob = null
        updateScanState(null)
    }

    private fun updateScanState(scanState: DocumentScanUiState?) {
        val current = _uiState.value as? LocationDetailUiState.Success ?: return
        _uiState.value = current.copy(scanState = scanState)
    }

    companion object {
        private const val AUTO_SAVE_DEBOUNCE_MS = 300L
    }
}

private fun Hotel.toEditState() = HotelEditState(
    id = id,
    name = name,
    address = address,
    reservationNumber = reservationNumber,
)

