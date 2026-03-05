package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import cat.company.wandervault.domain.model.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel for the Location Detail screen.
 *
 * Holds the [Destination] whose details are being viewed.
 *
 * @param destination The destination whose details are displayed.
 */
class LocationDetailViewModel(
    destination: Destination,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LocationDetailUiState(destination = destination))
    val uiState: StateFlow<LocationDetailUiState> = _uiState.asStateFlow()
}
