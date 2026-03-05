package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.usecase.GetDestinationByIdUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Location Detail screen.
 *
 * @param destinationId The ID of the destination whose details are displayed.
 * @param getDestinationById Use-case that fetches a single destination by ID.
 */
class LocationDetailViewModel(
    private val destinationId: Int,
    private val getDestinationById: GetDestinationByIdUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<LocationDetailUiState>(LocationDetailUiState.Loading)
    val uiState: StateFlow<LocationDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getDestinationById(destinationId).collect { destination ->
                _uiState.value = if (destination != null) {
                    LocationDetailUiState.Success(destination)
                } else {
                    LocationDetailUiState.Error
                }
            }
        }
    }
}
