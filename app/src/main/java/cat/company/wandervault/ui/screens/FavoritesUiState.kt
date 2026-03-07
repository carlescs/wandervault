package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Trip

/** UI state for the Favorites screen. */
data class FavoritesUiState(
    val trips: List<Trip> = emptyList(),
    val isLoading: Boolean = true,
) {
    /** `true` when loading is complete and there are no favorite trips to display. */
    val showEmptyState: Boolean get() = !isLoading && trips.isEmpty()
}
