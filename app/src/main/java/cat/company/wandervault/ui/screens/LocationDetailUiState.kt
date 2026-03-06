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
        /** The transport (with its legs) used to arrive at this destination (from the preceding stop). */
        val arrivalTransport: Transport? = null,
        /** `true` when this is the first (start) destination in the trip; no arrival transport applies. */
        val isFirst: Boolean = false,
        /** `true` when this is the last (end) destination in the trip; no departure transport applies. */
        val isLast: Boolean = false,
    ) : LocationDetailUiState()

    /** An error occurred (e.g. destination not found). */
    data object Error : LocationDetailUiState()
}
