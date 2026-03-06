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
 * Loads the destination by ID and manages inline editing of the transport leg.
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
                        val editState = if (current is TransportDetailUiState.Success) {
                            current.editState
                        } else {
                            TransportDetailEditState(
                                typeName = destination.transport?.type?.name,
                                company = destination.transport?.company ?: "",
                                flightNumber = destination.transport?.flightNumber ?: "",
                                confirmationNumber = destination.transport?.reservationConfirmationNumber ?: "",
                            )
                        }
                        _uiState.value = TransportDetailUiState.Success(
                            destinationName = destination.name,
                            editState = editState,
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

    /** Updates the selected transport type. */
    fun onTypeSelected(typeName: String?) {
        updateEditState { copy(typeName = typeName) }
    }

    /** Updates the company/carrier name field. */
    fun onCompanyChange(value: String) {
        updateEditState { copy(company = value) }
    }

    /** Updates the flight/reference number field. */
    fun onFlightNumberChange(value: String) {
        updateEditState { copy(flightNumber = value) }
    }

    /** Updates the reservation confirmation number field. */
    fun onConfirmationNumberChange(value: String) {
        updateEditState { copy(confirmationNumber = value) }
    }

    /**
     * Persists the current editing state to the database.
     *
     * - If a type is selected, the transport is saved or updated.
     * - If no type is selected and a transport exists, it is deleted.
     */
    fun onSave() {
        val destination = _lastDestination ?: return
        val state = _uiState.value as? TransportDetailUiState.Success ?: return
        val editState = state.editState
        val selectedType = editState.typeName?.let { name ->
            runCatching { TransportType.valueOf(name) }.getOrNull()
        }
        val existing: Transport? = destination.transport
        viewModelScope.launch {
            when {
                selectedType == null && existing != null -> deleteTransport(existing)
                selectedType != null && existing != null -> updateTransport(
                    existing.copy(
                        type = selectedType,
                        company = editState.company.trim().takeIf { it.isNotBlank() },
                        flightNumber = editState.flightNumber.trim().takeIf { it.isNotBlank() },
                        reservationConfirmationNumber = editState.confirmationNumber.trim().takeIf { it.isNotBlank() },
                    ),
                )
                selectedType != null -> saveTransport(
                    Transport(
                        id = 0,
                        destinationId = destination.id,
                        type = selectedType,
                        company = editState.company.trim().takeIf { it.isNotBlank() },
                        flightNumber = editState.flightNumber.trim().takeIf { it.isNotBlank() },
                        reservationConfirmationNumber = editState.confirmationNumber.trim().takeIf { it.isNotBlank() },
                    ),
                )
            }
        }
    }

    private inline fun updateEditState(update: TransportDetailEditState.() -> TransportDetailEditState) {
        val current = _uiState.value as? TransportDetailUiState.Success ?: return
        _uiState.value = current.copy(editState = current.editState.update())
    }
}
