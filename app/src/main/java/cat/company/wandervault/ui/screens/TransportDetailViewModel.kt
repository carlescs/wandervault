package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.usecase.DeleteTransportUseCase
import cat.company.wandervault.domain.usecase.DeleteTransportLegUseCase
import cat.company.wandervault.domain.usecase.GetDestinationByIdUseCase
import cat.company.wandervault.domain.usecase.GetDocumentByIdUseCase
import cat.company.wandervault.domain.usecase.GetNextDestinationUseCase
import cat.company.wandervault.domain.usecase.GetOrCreateTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.SaveTransportLegUseCase
import cat.company.wandervault.domain.usecase.UpdateDestinationUseCase
import cat.company.wandervault.domain.usecase.UpdateTransportLegUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

/**
 * ViewModel for the Transport Detail screen.
 *
 * Loads the destination by ID and manages inline editing of its transport legs.
 * Multiple legs can be added, edited, or removed. Changes are persisted automatically
 * after a short debounce delay; [onSave] can also be called explicitly (e.g., on navigate-away)
 * to flush any pending edits immediately.
 * Call [loadDestination] to start observing a destination.
 *
 * @param getDestinationById Use-case that fetches a destination by ID.
 * @param getOrCreateTransport Use-case that gets or creates the parent transport for a destination.
 * @param getNextDestination Use-case that fetches the next destination in the trip itinerary
 *   (used to resolve the destination name shown on the Details tab).
 * @param saveTransportLeg Use-case that persists a new transport leg.
 * @param updateTransportLeg Use-case that updates an existing transport leg.
 * @param deleteTransportLeg Use-case that removes a transport leg.
 * @param deleteTransport Use-case that removes the parent transport (and all legs via CASCADE).
 * @param updateDestination Use-case that updates a destination (used to sync the first leg's
 *   departure and the last leg's arrival with the corresponding destination date-times).
 * @param getDocumentById Use-case that resolves a [cat.company.wandervault.domain.model.TripDocument]
 *   by its ID; used to look up the name of the source document linked to a transport leg.
 */
class TransportDetailViewModel(
    private val getDestinationById: GetDestinationByIdUseCase,
    private val getOrCreateTransport: GetOrCreateTransportForDestinationUseCase,
    private val getNextDestination: GetNextDestinationUseCase,
    private val saveTransportLeg: SaveTransportLegUseCase,
    private val updateTransportLeg: UpdateTransportLegUseCase,
    private val deleteTransportLeg: DeleteTransportLegUseCase,
    private val deleteTransport: DeleteTransportUseCase,
    private val updateDestination: UpdateDestinationUseCase,
    private val getDocumentById: GetDocumentByIdUseCase,
) : ViewModel() {

    private val _destinationId = MutableStateFlow<Int?>(null)

    /** The last destination snapshot received from the database (used in [onSave]). */
    private var _lastDestination: Destination? = null

    /** The last next-destination snapshot received from the database (used to sync last-leg arrival). */
    private var _lastNextDestination: Destination? = null

    /**
     * True when the user has made unsaved edits.  While true, incoming DB emissions do not
     * overwrite the edit state so in-progress work is preserved.  Reset to false after
     * [onSave] so that the next DB emission refreshes the legs with the real persisted IDs,
     * preventing duplicate inserts on a second save.
     */
    private var _hasUnsavedEdits = false

    /**
     * Counter used to assign unique negative [TransportLegEditState.clientKey] values to new
     * (unsaved) legs.  Negative values avoid collision with positive database IDs.
     */
    private var _nextClientKey = -1

    private val _uiState = MutableStateFlow<TransportDetailUiState>(TransportDetailUiState.Loading)
    val uiState: StateFlow<TransportDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _destinationId
                .filterNotNull()
                .flatMapLatest { id -> getDestinationById(id) }
                .flatMapLatest { destination ->
                    if (destination == null) {
                        flowOf(Pair<Destination?, Destination?>(null, null))
                    } else {
                        getNextDestination(destination).map { nextDest ->
                            Pair(destination, nextDest)
                        }
                    }
                }
                .collect { (destination, nextDestination) ->
                    if (destination == null) {
                        _uiState.value = TransportDetailUiState.Error
                    } else {
                        _lastDestination = destination
                        _lastNextDestination = nextDestination
                        // Preserve in-progress edits; only remap from the DB when there are none.
                        val legs = if (_hasUnsavedEdits && _uiState.value is TransportDetailUiState.Success) {
                            (_uiState.value as TransportDetailUiState.Success).legs
                        } else {
                            val dbLegs = destination.transport?.legs ?: emptyList()
                            dbLegs.mapIndexed { idx, leg ->
                                val sourceDocName = leg.sourceDocumentId?.let { docId ->
                                    getDocumentById(docId).first()?.name
                                }
                                leg.toEditState(
                                    isFirst = idx == 0,
                                    destinationDepartureDateTime = destination.departureDateTime,
                                    isLast = idx == dbLegs.lastIndex,
                                    nextArrivalDateTime = nextDestination?.arrivalDateTime,
                                    sourceDocumentName = sourceDocName,
                                )
                            }
                        }
                        _uiState.value = TransportDetailUiState.Success(
                            destinationName = destination.name,
                            originName = destination.name,
                            nextDestinationName = nextDestination?.name,
                            legs = legs,
                            destinationDepartureDateTime = destination.departureDateTime,
                            nextDestinationArrivalDateTime = nextDestination?.arrivalDateTime,
                        )
                    }
                }
        }

        // Auto-save: persist changes to the database after a short delay once editing stops.
        // collectLatest cancels any in-flight save when a new state emission arrives, so only
        // the latest edit snapshot is persisted and DB operations are never interleaved.
        viewModelScope.launch {
            _uiState
                .debounce(AUTO_SAVE_DEBOUNCE_MS)
                .collectLatest { persistLegs() }
        }
    }

    /** Switch the screen to display transport for the destination with the given [id]. */
    fun loadDestination(id: Int) {
        _uiState.value = TransportDetailUiState.Loading
        _lastDestination = null
        _lastNextDestination = null
        _hasUnsavedEdits = false
        // Force a new emission even if the same ID is loaded again (StateFlow deduplicates equal values).
        _destinationId.value = null
        _destinationId.value = id
    }

    /** Adds a new empty leg to the end of the list, inheriting the type of the last existing leg. */
    fun onAddLeg() {
        _hasUnsavedEdits = true
        val lastTypeName = (_uiState.value as? TransportDetailUiState.Success)?.legs?.lastOrNull()?.typeName
        updateLegs { this + TransportLegEditState(clientKey = _nextClientKey--, typeName = lastTypeName) }
    }

    /** Removes the leg at the given [index]. */
    fun onRemoveLeg(index: Int) {
        updateLegs {
            val mutable = toMutableList()
            if (index in mutable.indices) {
                // Only mark dirty if the list actually changes (no-op for out-of-range indices).
                _hasUnsavedEdits = true
                mutable.removeAt(index)
                mutable
            } else {
                this
            }
        }
    }

    /** Updates the selected transport type for the leg at [index]. */
    fun onTypeSelected(index: Int, typeName: String?) {
        _hasUnsavedEdits = true
        updateLeg(index) { copy(typeName = typeName) }
    }

    /** Updates the stop name field for the leg at [index]. */
    fun onStopNameChange(index: Int, value: String) {
        _hasUnsavedEdits = true
        updateLeg(index) { copy(stopName = value) }
    }

    /** Updates the company/carrier name field for the leg at [index]. */
    fun onCompanyChange(index: Int, value: String) {
        _hasUnsavedEdits = true
        updateLeg(index) { copy(company = value) }
    }

    /** Updates the flight/reference number field for the leg at [index]. */
    fun onFlightNumberChange(index: Int, value: String) {
        _hasUnsavedEdits = true
        updateLeg(index) { copy(flightNumber = value) }
    }

    /** Updates the reservation confirmation number field for the leg at [index]. */
    fun onConfirmationNumberChange(index: Int, value: String) {
        _hasUnsavedEdits = true
        updateLeg(index) { copy(confirmationNumber = value) }
    }

    /**
     * Updates the departure date/time for the leg at [index].
     *
     * When [index] is 0 (the first leg), also persists the new value to the parent destination's
     * [Destination.departureDateTime] so that the itinerary timeline stays in sync.
     */
    fun onDepartureDateTimeChange(index: Int, dateTime: ZonedDateTime?) {
        _hasUnsavedEdits = true
        updateLeg(index) { copy(departureDateTime = dateTime) }
        if (index == 0) {
            val destination = _lastDestination ?: return
            viewModelScope.launch {
                updateDestination(destination.copy(departureDateTime = dateTime))
                _lastDestination = destination.copy(departureDateTime = dateTime)
            }
        }
    }

    /**
     * Updates the arrival date/time for the leg at [index].
     *
     * When [index] is the last leg, also persists the new value to the next destination's
     * [Destination.arrivalDateTime] so that the itinerary timeline stays in sync.
     */
    fun onArrivalDateTimeChange(index: Int, dateTime: ZonedDateTime?) {
        _hasUnsavedEdits = true
        updateLeg(index) { copy(arrivalDateTime = dateTime) }
        val current = _uiState.value as? TransportDetailUiState.Success ?: return
        if (current.legs.isNotEmpty() && index == current.legs.lastIndex) {
            val nextDestination = _lastNextDestination ?: return
            viewModelScope.launch {
                updateDestination(nextDestination.copy(arrivalDateTime = dateTime))
                _lastNextDestination = nextDestination.copy(arrivalDateTime = dateTime)
            }
        }
    }

    /**
     * Sets the leg at [index] as the default leg for the timeline icon.
     * All other legs have their [TransportLegEditState.isDefault] cleared to `false`.
     */
    fun onSetDefaultLeg(index: Int) {
        _hasUnsavedEdits = true
        val current = _uiState.value as? TransportDetailUiState.Success ?: return
        if (index !in current.legs.indices) return
        _uiState.value = current.copy(
            legs = current.legs.mapIndexed { i, leg -> leg.copy(isDefault = i == index) },
        )
    }

    /** Moves the leg at [index] one position up (swaps with the leg at [index] - 1). */
    fun onMoveLegUp(index: Int) {
        updateLegs {
            val swapped = swapLegs(index, index - 1)
            if (swapped !== this) _hasUnsavedEdits = true
            swapped
        }
    }

    /** Moves the leg at [index] one position down (swaps with the leg at [index] + 1). */
    fun onMoveLegDown(index: Int) {
        updateLegs {
            val swapped = swapLegs(index, index + 1)
            if (swapped !== this) _hasUnsavedEdits = true
            swapped
        }
    }

    /**
     * Triggers a save of the current leg edits.  This is called explicitly when the user
     * navigates away to flush any edits that fall inside the debounce window.  Actual
     * persistence is delegated to the suspending [persistLegs] function.
     */
    fun onSave() {
        viewModelScope.launch { persistLegs() }
    }

    /**
     * Removes the source document link for the leg at [index].
     * The change is picked up by the auto-save debounce and persisted to the database.
     */
    fun onClearLegSourceDocument(index: Int) {
        val current = _uiState.value as? TransportDetailUiState.Success ?: return
        if (index !in current.legs.indices) return
        val leg = current.legs[index]
        if (leg.sourceDocumentId == null) return
        _hasUnsavedEdits = true
        updateLeg(index) { copy(sourceDocumentId = null, sourceDocumentName = null) }
    }

    /**
     * Suspending implementation that persists the current list of legs to the database.
     *
     * Being suspending lets [collectLatest] cancel an in-flight save if new edits arrive,
     * ensuring DB operations from two concurrent save paths never interleave.
     *
     * - If there are no unsaved edits, this is a no-op.
     * - If all legs are empty/cleared, the parent transport is deleted (which cascade-deletes legs).
     * - Otherwise, the parent transport is created if it doesn't exist yet, and legs are
     *   inserted, updated, or deleted as required.
     * - Legs with no transport type selected are skipped (treated as empty).
     */
    private suspend fun persistLegs() {
        if (!_hasUnsavedEdits) return
        val destination = _lastDestination ?: return
        val state = _uiState.value as? TransportDetailUiState.Success ?: return
        // Clear the dirty flag before persisting so the next DB emission will remap from
        // the real persisted IDs, preventing duplicate inserts on a second save.
        _hasUnsavedEdits = false

        val validLegs = state.legs.filter { leg ->
            leg.typeName?.let { runCatching { TransportType.valueOf(it) }.getOrNull() } != null
        }

        if (validLegs.isEmpty()) {
            // No valid legs left – delete the parent transport if it exists.
            destination.transport?.let { deleteTransport(it) }
            return
        }

        // Get or create the parent transport for this destination.
        val transportId = getOrCreateTransport(destination.id)

        val existingLegsById = destination.transport?.legs?.associateBy { it.id } ?: emptyMap()
        val editedIds = mutableSetOf<Int>()

        state.legs.forEachIndexed { position, leg ->
            val selectedType = leg.typeName?.let { name ->
                runCatching { TransportType.valueOf(name) }.getOrNull()
            } ?: return@forEachIndexed

            // The last leg ends at the final destination; its stopName is not editable and
            // should always be stored as null.
            val stopName = if (position == state.legs.lastIndex) {
                null
            } else {
                leg.stopName.trim().takeIf { it.isNotBlank() }
            }

            if (leg.id > 0) {
                editedIds.add(leg.id)
                val existing = existingLegsById[leg.id]
                if (existing != null) {
                    updateTransportLeg(
                        existing.copy(
                            type = selectedType,
                            position = position,
                            stopName = stopName,
                            company = leg.company.trim().takeIf { it.isNotBlank() },
                            flightNumber = leg.flightNumber.trim().takeIf { it.isNotBlank() },
                            reservationConfirmationNumber = leg.confirmationNumber.trim().takeIf { it.isNotBlank() },
                            isDefault = leg.isDefault,
                            departureDateTime = leg.departureDateTime,
                            arrivalDateTime = leg.arrivalDateTime,
                            sourceDocumentId = leg.sourceDocumentId,
                        ),
                    )
                }
            } else {
                saveTransportLeg(
                    TransportLeg(
                        id = 0,
                        transportId = transportId,
                        type = selectedType,
                        position = position,
                        stopName = stopName,
                        company = leg.company.trim().takeIf { it.isNotBlank() },
                        flightNumber = leg.flightNumber.trim().takeIf { it.isNotBlank() },
                        reservationConfirmationNumber = leg.confirmationNumber.trim().takeIf { it.isNotBlank() },
                        isDefault = leg.isDefault,
                        departureDateTime = leg.departureDateTime,
                        arrivalDateTime = leg.arrivalDateTime,
                    ),
                )
            }
        }

        // Delete any existing legs that were removed from the edit list.
        destination.transport?.legs
            ?.filter { it.id !in editedIds }
            ?.forEach { deleteTransportLeg(it) }
    }

    private inline fun updateLegs(update: List<TransportLegEditState>.() -> List<TransportLegEditState>) {
        val current = _uiState.value as? TransportDetailUiState.Success ?: return
        _uiState.value = current.copy(legs = current.legs.update())
    }

    private inline fun updateLeg(index: Int, update: TransportLegEditState.() -> TransportLegEditState) {
        val current = _uiState.value as? TransportDetailUiState.Success ?: return
        if (index !in current.legs.indices) return
        _uiState.value = current.copy(
            legs = current.legs.toMutableList().also { it[index] = it[index].update() },
        )
    }

    /**
     * Returns a new list with the elements at [indexA] and [indexB] swapped.
     * If either index is out of range the original list is returned unchanged.
     */
    private fun List<TransportLegEditState>.swapLegs(
        indexA: Int,
        indexB: Int,
    ): List<TransportLegEditState> {
        if (indexA !in indices || indexB !in indices) return this
        val mutable = toMutableList()
        val tmp = mutable[indexA]
        mutable[indexA] = mutable[indexB]
        mutable[indexB] = tmp
        return mutable
    }

    companion object {
        /** Debounce delay in milliseconds before auto-saving leg edits to the database. */
        private const val AUTO_SAVE_DEBOUNCE_MS = 300L
    }
}

private fun TransportLeg.toEditState(
    isFirst: Boolean = false,
    destinationDepartureDateTime: ZonedDateTime? = null,
    isLast: Boolean = false,
    nextArrivalDateTime: ZonedDateTime? = null,
    sourceDocumentName: String? = null,
) = TransportLegEditState(
    id = id,
    clientKey = id,
    typeName = type.name,
    stopName = stopName ?: "",
    company = company ?: "",
    flightNumber = flightNumber ?: "",
    confirmationNumber = reservationConfirmationNumber ?: "",
    isDefault = isDefault,
    // use the destination value as the authoritative source when loading, but only when
    // a non-null value is available to avoid clobbering an existing leg date-time.
    departureDateTime = if (isFirst && destinationDepartureDateTime != null) {
        destinationDepartureDateTime
    } else {
        departureDateTime
    },
    // The last leg's arrival is always kept in sync with the next destination's arrival;
    // use the next destination value as the authoritative source when loading, but only when
    // a non-null value is available to avoid clobbering an existing leg date-time.
    arrivalDateTime = if (isLast && nextArrivalDateTime != null) {
        nextArrivalDateTime
    } else {
        arrivalDateTime
    },
    sourceDocumentId = sourceDocumentId,
    sourceDocumentName = sourceDocumentName,
)

