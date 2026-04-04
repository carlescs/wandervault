package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Trip

/** UI state for the Archive screen. */
data class ArchiveUiState(
    val trips: List<Trip> = emptyList(),
    val isLoading: Boolean = true,
) {
    /** `true` when loading is complete and there are no archived trips to display. */
    val showEmptyState: Boolean get() = !isLoading && trips.isEmpty()
}
