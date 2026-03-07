package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Trip

/** UI state for the Favorites screen. */
data class FavoritesUiState(
    val trips: List<Trip> = emptyList(),
    val isLoading: Boolean = true,
)
