package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.usecase.GetArrivalTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetDestinationByIdUseCase
import cat.company.wandervault.domain.usecase.GetDestinationsForTripUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * ViewModel for the Location Detail screen.
 *
 * A single instance is reused across destinations. Call [loadDestination] whenever the
 * displayed destination changes; the ViewModel switches its collection to the new ID via
 * [flatMapLatest], cancelling the previous subscription automatically.
 *
 * @param getDestinationById Use-case that fetches a single destination by ID.
 * @param getArrivalTransport Use-case that fetches the transport used to arrive at a destination.
 * @param getDestinationsForTrip Use-case that fetches all destinations for a trip, used to
 *   determine whether the destination is the first or last stop.
 */
class LocationDetailViewModel(
    private val getDestinationById: GetDestinationByIdUseCase,
    private val getArrivalTransport: GetArrivalTransportForDestinationUseCase,
    private val getDestinationsForTrip: GetDestinationsForTripUseCase,
) : ViewModel() {

    private val _destinationId = MutableStateFlow<Int?>(null)

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
                    ) { destination, arrivalTransport ->
                        destination to arrivalTransport
                    }.flatMapLatest { (destination, arrivalTransport) ->
                        if (destination == null) {
                            flowOf(LocationDetailUiState.Error)
                        } else {
                            getDestinationsForTrip(destination.tripId).map { tripDestinations ->
                                val firstPosition = tripDestinations.firstOrNull()?.position
                                val lastPosition = tripDestinations.lastOrNull()?.position
                                val isFirst = destination.position == firstPosition
                                val isLast = destination.position == lastPosition
                                LocationDetailUiState.Success(
                                    destination = destination,
                                    arrivalTransport = arrivalTransport,
                                    isFirst = isFirst,
                                    isLast = isLast,
                                )
                            }
                        }
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    /** Switch the screen to display the destination with the given [id]. */
    fun loadDestination(id: Int) {
        _uiState.value = LocationDetailUiState.Loading
        _destinationId.value = id
    }
}
