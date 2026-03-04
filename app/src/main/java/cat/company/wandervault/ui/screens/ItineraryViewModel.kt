package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.usecase.DeleteDestinationUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.SaveDestinationUseCase
import cat.company.wandervault.domain.usecase.UpdateDestinationUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * ViewModel for the Itinerary tab on the Trip Detail screen.
 *
 * Manages the ordered list of [Destination]s for a given trip and exposes
 * actions to add, update, and delete destinations.
 *
 * @param tripId The ID of the trip whose itinerary is displayed.
 * @param getDestinationsForTrip Use-case that returns a live list of destinations.
 * @param saveDestination Use-case that persists a new destination.
 * @param updateDestination Use-case that updates an existing destination.
 * @param deleteDestination Use-case that removes a destination.
 */
class ItineraryViewModel(
    private val tripId: Int,
    private val getDestinationsForTrip: GetDestinationsForTripUseCase,
    private val saveDestination: SaveDestinationUseCase,
    private val updateDestination: UpdateDestinationUseCase,
    private val deleteDestination: DeleteDestinationUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItineraryUiState())
    val uiState: StateFlow<ItineraryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getDestinationsForTrip(tripId).collect { destinations ->
                _uiState.update { it.copy(destinations = destinations, isLoading = false) }
            }
        }
    }

    fun onAddDestinationClick(insertAfterPosition: Int? = null) {
        _uiState.update { it.copy(showAddDestinationDialog = true, insertAfterPosition = insertAfterPosition) }
    }

    fun onDismissAddDestinationDialog() {
        _uiState.update { it.copy(showAddDestinationDialog = false, newDestinationName = "", insertAfterPosition = null) }
    }

    fun onNewDestinationNameChange(name: String) {
        _uiState.update { it.copy(newDestinationName = name) }
    }

    fun onSaveDestination() {
        val state = _uiState.value
        if (!state.isAddDestinationFormValid) return
        viewModelScope.launch {
            val insertPos = if (state.insertAfterPosition == null) {
                state.destinations.maxOfOrNull { it.position }?.plus(1) ?: 0
            } else {
                state.insertAfterPosition + 1
            }
            // Shift existing destinations at or after the insertion point to make room.
            state.destinations
                .filter { it.position >= insertPos }
                .map { dest -> async { updateDestination(dest.copy(position = dest.position + 1)) } }
                .awaitAll()
            saveDestination(
                Destination(
                    tripId = tripId,
                    name = state.newDestinationName.trim(),
                    position = insertPos,
                ),
            )
            onDismissAddDestinationDialog()
        }
    }

    fun onUpdateArrivalDateTime(destination: Destination, arrivalDateTime: LocalDateTime?) {
        viewModelScope.launch {
            updateDestination(destination.copy(arrivalDateTime = arrivalDateTime))
        }
    }

    fun onUpdateDepartureDateTime(destination: Destination, departureDateTime: LocalDateTime?) {
        viewModelScope.launch {
            updateDestination(destination.copy(departureDateTime = departureDateTime))
        }
    }

    fun onUpdateTransport(destination: Destination, transport: TransportType?) {
        viewModelScope.launch {
            updateDestination(destination.copy(transport = transport))
        }
    }

    fun onMoveDestinationUp(destination: Destination) {
        viewModelScope.launch {
            val destinations = _uiState.value.destinations
            val index = destinations.indexOfFirst { it.id == destination.id }
            if (index <= 0) return@launch
            val swapWith = destinations[index - 1]
            // After the swap: destination is at index-1, swapWith is at index.
            val isDestinationNowFirst = index - 1 == 0
            val isSwapWithNowLast = index == destinations.lastIndex
            awaitAll(
                async {
                    updateDestination(
                        destination.copy(
                            position = swapWith.position,
                            arrivalDateTime = if (isDestinationNowFirst) null else destination.arrivalDateTime,
                        ),
                    )
                },
                async {
                    updateDestination(
                        swapWith.copy(
                            position = destination.position,
                            departureDateTime = if (isSwapWithNowLast) null else swapWith.departureDateTime,
                            transport = if (isSwapWithNowLast) null else swapWith.transport,
                        ),
                    )
                },
            )
        }
    }

    fun onMoveDestinationDown(destination: Destination) {
        viewModelScope.launch {
            val destinations = _uiState.value.destinations
            val index = destinations.indexOfFirst { it.id == destination.id }
            if (index >= destinations.lastIndex) return@launch
            val swapWith = destinations[index + 1]
            // After the swap: destination is at index+1, swapWith is at index.
            val isDestinationNowLast = index + 1 == destinations.lastIndex
            val isSwapWithNowFirst = index == 0
            awaitAll(
                async {
                    updateDestination(
                        destination.copy(
                            position = swapWith.position,
                            departureDateTime = if (isDestinationNowLast) null else destination.departureDateTime,
                            transport = if (isDestinationNowLast) null else destination.transport,
                        ),
                    )
                },
                async {
                    updateDestination(
                        swapWith.copy(
                            position = destination.position,
                            arrivalDateTime = if (isSwapWithNowFirst) null else swapWith.arrivalDateTime,
                        ),
                    )
                },
            )
        }
    }

    fun onDeleteDestination(destination: Destination) {
        _uiState.update { it.copy(destinationPendingDelete = destination) }
    }

    fun onDismissDeleteDestinationDialog() {
        _uiState.update { it.copy(destinationPendingDelete = null) }
    }

    fun onConfirmDeleteDestination() {
        val destination = _uiState.value.destinationPendingDelete ?: return
        _uiState.update { it.copy(destinationPendingDelete = null) }
        viewModelScope.launch {
            // Capture remaining list inside the coroutine (before the delete) to ensure
            // we're working with the latest state.
            val remaining = _uiState.value.destinations
                .filter { it.id != destination.id }
                .sortedBy { it.position }
            deleteDestination(destination)
            // Re-sequence positions and clear date-times that no longer apply to the
            // new first (no arrival) and new last (no departure) destinations.
            // Updates are issued in parallel to avoid partially-visible intermediate states.
            remaining.mapIndexedNotNull { index, dest ->
                val isNowFirst = index == 0
                val isNowLast = index == remaining.lastIndex
                val needsReindex = dest.position != index
                val needsArrivalClear = isNowFirst && dest.arrivalDateTime != null
                val needsDepartureClear = isNowLast && dest.departureDateTime != null
                val needsTransportClear = isNowLast && dest.transport != null
                if (needsReindex || needsArrivalClear || needsDepartureClear || needsTransportClear) {
                    dest.copy(
                        position = index,
                        arrivalDateTime = if (isNowFirst) null else dest.arrivalDateTime,
                        departureDateTime = if (isNowLast) null else dest.departureDateTime,
                        transport = if (isNowLast) null else dest.transport,
                    )
                } else null
            }.map { updated -> async { updateDestination(updated) } }.awaitAll()
        }
    }
}
