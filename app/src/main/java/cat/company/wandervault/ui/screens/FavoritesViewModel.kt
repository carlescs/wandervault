package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.usecase.GetFavoriteTripsUseCase
import cat.company.wandervault.domain.usecase.ToggleFavoriteTripUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FavoritesViewModel(
    private val getFavoriteTrips: GetFavoriteTripsUseCase,
    private val toggleFavorite: ToggleFavoriteTripUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FavoritesUiState())
    val uiState: StateFlow<FavoritesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getFavoriteTrips().collect { trips ->
                _uiState.update { it.copy(trips = trips, isLoading = false) }
            }
        }
    }

    fun onToggleFavorite(trip: Trip) {
        viewModelScope.launch {
            toggleFavorite(trip)
        }
    }
}
