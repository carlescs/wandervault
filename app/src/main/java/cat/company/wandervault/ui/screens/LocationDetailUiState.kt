package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Destination

/**
 * UI state for the Location Detail screen.
 *
 * @param destination The destination being viewed.
 */
data class LocationDetailUiState(
    val destination: Destination,
)
