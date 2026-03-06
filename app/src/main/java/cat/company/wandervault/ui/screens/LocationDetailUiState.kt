package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Transport

/**
 * Represents the UI state for the Location Detail screen.
 */
sealed class LocationDetailUiState {
    /** The destination details are being loaded. */
    data object Loading : LocationDetailUiState()

    /** The destination was loaded successfully. */
    data class Success(
        val destination: Destination,
        /** The transport legs used to arrive at this destination (from the preceding stop). */
        val arrivalTransports: List<Transport> = emptyList(),
    ) : LocationDetailUiState()

    /** An error occurred (e.g. destination not found). */
    data object Error : LocationDetailUiState()
}
