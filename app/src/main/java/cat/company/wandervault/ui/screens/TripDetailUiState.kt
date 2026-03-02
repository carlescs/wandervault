package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Trip

/**
 * Represents the UI state for the Trip Detail screen.
 */
sealed class TripDetailUiState {
    /** The detail is being loaded. */
    data object Loading : TripDetailUiState()

    /** The trip was loaded successfully. */
    data class Success(val trip: Trip) : TripDetailUiState()

    /** An error occurred (e.g. trip not found). */
    data class Error(val message: String) : TripDetailUiState()
}
