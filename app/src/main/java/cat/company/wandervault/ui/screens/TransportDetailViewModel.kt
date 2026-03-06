package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.usecase.DeleteTransportUseCase
import cat.company.wandervault.domain.usecase.DeleteTransportLegUseCase
import cat.company.wandervault.domain.usecase.GetDestinationByIdUseCase
import cat.company.wandervault.domain.usecase.GetNextDestinationUseCase
import cat.company.wandervault.domain.usecase.GetOrCreateTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.SaveTransportLegUseCase
import cat.company.wandervault.domain.usecase.UpdateTransportLegUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
 */
class TransportDetailViewModel(
    private val getDestinationById: GetDestinationByIdUseCase,
    private val getOrCreateTransport: GetOrCreateTransportForDestinationUseCase,
    private val getNextDestination: GetNextDestinationUseCase,
    private val saveTransportLeg: SaveTransportLegUseCase,
    private val updateTransportLeg: UpdateTransportLegUseCase,
    private val deleteTransportLeg: DeleteTransportLegUseCase,
    private val deleteTransport: DeleteTransportUseCase,
) : ViewModel() {

    private val _destinationId = MutableStateFlow<Int?>(null)

    /** The last destination snapshot received from the database (used in [onSave]). */
    private var _lastDestination: Destination? = null

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
                        // Preserve in-progress edits; only remap from the DB when there are none.
                        val legs = if (_hasUnsavedEdits && _uiState.value is TransportDetailUiState.Success) {
                            (_uiState.value as TransportDetailUiState.Success).legs
                        } else {
                            destination.transport?.legs?.map { it.toEditState() } ?: emptyList()
                        }
                        _uiState.value = TransportDetailUiState.Success(
                            destinationName = destination.name,
                            originName = destination.name,
                            nextDestinationName = nextDestination?.name,
                            legs = legs,
                        )
                    }
                }
        }

        // Auto-save: persist changes to the database after a short delay once editing stops.
        viewModelScope.launch {
            _uiState
                .debounce(AUTO_SAVE_DEBOUNCE_MS)
                .collect { onSave() }
        }
    }

    /** Switch the screen to display transport for the destination with the given [id]. */
    fun loadDestination(id: Int) {
        _uiState.value = TransportDetailUiState.Loading
        _lastDestination = null
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
     * Persists the current list of legs to the database.
     *
     * This is called automatically by the debounced auto-save, and also explicitly when the
     * user navigates away to flush any edits that were made within the debounce window.
     *
     * - If all legs are empty/cleared, the parent transport is deleted (which cascade-deletes legs).
     * - Otherwise, the parent transport is created if it doesn't exist yet, and legs are
     *   inserted, updated, or deleted as required.
     * - Legs with no transport type selected are skipped (treated as empty).
     */
    fun onSave() {
        if (!_hasUnsavedEdits) return
        val destination = _lastDestination ?: return
        val state = _uiState.value as? TransportDetailUiState.Success ?: return
        // Clear the dirty flag before persisting so the next DB emission will remap from
        // the real persisted IDs, preventing duplicate inserts on a second save.
        _hasUnsavedEdits = false
        viewModelScope.launch {
            val validLegs = state.legs.filter { leg ->
                leg.typeName?.let { runCatching { TransportType.valueOf(it) }.getOrNull() } != null
            }

            if (validLegs.isEmpty()) {
                // No valid legs left – delete the parent transport if it exists.
                destination.transport?.let { deleteTransport(it) }
                return@launch
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
                        ),
                    )
                }
            }

            // Delete any existing legs that were removed from the edit list.
            destination.transport?.legs
                ?.filter { it.id !in editedIds }
                ?.forEach { deleteTransportLeg(it) }
        }
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

private fun TransportLeg.toEditState() = TransportLegEditState(
    id = id,
    clientKey = id,
    typeName = type.name,
    stopName = stopName ?: "",
    company = company ?: "",
    flightNumber = flightNumber ?: "",
    confirmationNumber = reservationConfirmationNumber ?: "",
)

