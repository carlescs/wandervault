package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.usecase.GetArrivalTransportForDestinationUseCase
import cat.company.wandervault.domain.usecase.GetDestinationByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
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
 */
class LocationDetailViewModel(
    private val getDestinationById: GetDestinationByIdUseCase,
    private val getArrivalTransport: GetArrivalTransportForDestinationUseCase,
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
                    ) { destination, arrivalTransports ->
                        if (destination != null) {
                            LocationDetailUiState.Success(destination, arrivalTransports)
                        } else {
                            LocationDetailUiState.Error
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
