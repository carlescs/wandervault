package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.usecase.DeleteTransportUseCase
import cat.company.wandervault.domain.usecase.GetDestinationByIdUseCase
import cat.company.wandervault.domain.usecase.SaveTransportUseCase
import cat.company.wandervault.domain.usecase.UpdateTransportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the Transport Detail screen.
 *
 * Loads the destination by ID and manages inline editing of its transport legs.
 * Multiple legs can be added, edited, or removed before saving.
 * Call [loadDestination] to start observing a destination.
 *
 * @param getDestinationById Use-case that fetches a destination by ID.
 * @param saveTransport Use-case that persists a new transport leg.
 * @param updateTransport Use-case that updates an existing transport leg.
 * @param deleteTransport Use-case that removes a transport leg.
 */
class TransportDetailViewModel(
    private val getDestinationById: GetDestinationByIdUseCase,
    private val saveTransport: SaveTransportUseCase,
    private val updateTransport: UpdateTransportUseCase,
    private val deleteTransport: DeleteTransportUseCase,
) : ViewModel() {

    private val _destinationId = MutableStateFlow<Int?>(null)

    /** The last destination snapshot received from the database (used in [onSave]). */
    private var _lastDestination: Destination? = null

    private val _uiState = MutableStateFlow<TransportDetailUiState>(TransportDetailUiState.Loading)
    val uiState: StateFlow<TransportDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _destinationId
                .filterNotNull()
                .flatMapLatest { id -> getDestinationById(id) }
                .collect { destination ->
                    if (destination == null) {
                        _uiState.value = TransportDetailUiState.Error
                    } else {
                        _lastDestination = destination
                        val current = _uiState.value
                        // Preserve any in-progress edits already made by the user.
                        val legs = if (current is TransportDetailUiState.Success) {
                            current.legs
                        } else {
                            destination.transports.map { it.toEditState() }
                        }
                        _uiState.value = TransportDetailUiState.Success(
                            destinationName = destination.name,
                            legs = legs,
                        )
                    }
                }
        }
    }

    /** Switch the screen to display transport for the destination with the given [id]. */
    fun loadDestination(id: Int) {
        _uiState.value = TransportDetailUiState.Loading
        _lastDestination = null
        // Force a new emission even if the same ID is loaded again (StateFlow deduplicates equal values).
        _destinationId.value = null
        _destinationId.value = id
    }

    /** Adds a new empty leg to the end of the list. */
    fun onAddLeg() {
        updateLegs { this + TransportLegEditState() }
    }

    /** Removes the leg at the given [index]. */
    fun onRemoveLeg(index: Int) {
        updateLegs { toMutableList().also { it.removeAt(index) } }
    }

    /** Updates the selected transport type for the leg at [index]. */
    fun onTypeSelected(index: Int, typeName: String?) {
        updateLeg(index) { copy(typeName = typeName) }
    }

    /** Updates the company/carrier name field for the leg at [index]. */
    fun onCompanyChange(index: Int, value: String) {
        updateLeg(index) { copy(company = value) }
    }

    /** Updates the flight/reference number field for the leg at [index]. */
    fun onFlightNumberChange(index: Int, value: String) {
        updateLeg(index) { copy(flightNumber = value) }
    }

    /** Updates the reservation confirmation number field for the leg at [index]. */
    fun onConfirmationNumberChange(index: Int, value: String) {
        updateLeg(index) { copy(confirmationNumber = value) }
    }

    /**
     * Persists the current list of legs to the database.
     *
     * - New legs (id == 0 and a type selected) are inserted.
     * - Existing legs (id > 0) are updated.
     * - Legs that were present in [_lastDestination] but are no longer in the edit list are deleted.
     * - Legs with no transport type selected are skipped (treated as empty).
     */
    fun onSave() {
        val destination = _lastDestination ?: return
        val state = _uiState.value as? TransportDetailUiState.Success ?: return
        viewModelScope.launch {
            val existingById = destination.transports.associateBy { it.id }
            val editedIds = mutableSetOf<Int>()

            state.legs.forEachIndexed { position, leg ->
                val selectedType = leg.typeName?.let { name ->
                    runCatching { TransportType.valueOf(name) }.getOrNull()
                } ?: return@forEachIndexed

                if (leg.id > 0) {
                    editedIds.add(leg.id)
                    val existing = existingById[leg.id]
                    if (existing != null) {
                        updateTransport(
                            existing.copy(
                                type = selectedType,
                                position = position,
                                company = leg.company.trim().takeIf { it.isNotBlank() },
                                flightNumber = leg.flightNumber.trim().takeIf { it.isNotBlank() },
                                reservationConfirmationNumber = leg.confirmationNumber.trim().takeIf { it.isNotBlank() },
                            ),
                        )
                    }
                } else {
                    saveTransport(
                        Transport(
                            id = 0,
                            destinationId = destination.id,
                            type = selectedType,
                            position = position,
                            company = leg.company.trim().takeIf { it.isNotBlank() },
                            flightNumber = leg.flightNumber.trim().takeIf { it.isNotBlank() },
                            reservationConfirmationNumber = leg.confirmationNumber.trim().takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }

            // Delete any existing legs that were removed from the edit list.
            destination.transports
                .filter { it.id !in editedIds }
                .forEach { deleteTransport(it) }
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
}

private fun Transport.toEditState() = TransportLegEditState(
    id = id,
    typeName = type.name,
    company = company ?: "",
    flightNumber = flightNumber ?: "",
    confirmationNumber = reservationConfirmationNumber ?: "",
)
