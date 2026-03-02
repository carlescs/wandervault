package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.usecase.DeleteDestinationUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import cat.company.wandervault.domain.usecase.SaveDestinationUseCase
import cat.company.wandervault.domain.usecase.UpdateDestinationUseCase
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

    fun onAddDestinationClick() {
        _uiState.update { it.copy(showAddDestinationDialog = true) }
    }

    fun onDismissAddDestinationDialog() {
        _uiState.update { it.copy(showAddDestinationDialog = false, newDestinationName = "") }
    }

    fun onNewDestinationNameChange(name: String) {
        _uiState.update { it.copy(newDestinationName = name) }
    }

    fun onSaveDestination() {
        val state = _uiState.value
        if (!state.isAddDestinationFormValid) return
        val nextPosition = state.destinations.maxOfOrNull { it.position }?.plus(1) ?: 0
        viewModelScope.launch {
            saveDestination(
                Destination(
                    tripId = tripId,
                    name = state.newDestinationName.trim(),
                    position = nextPosition,
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

    fun onDeleteDestination(destination: Destination) {
        viewModelScope.launch {
            deleteDestination(destination)
        }
    }
}
