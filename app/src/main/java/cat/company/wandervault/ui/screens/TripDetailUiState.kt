package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Trip

/**
 * Represents the UI state for the Trip Detail screen.
 */
sealed class TripDetailUiState {
    /** The detail is being loaded. */
    data object Loading : TripDetailUiState()

    /** The trip was loaded successfully. */
    data class Success(
        val trip: Trip,
        val descriptionState: DescriptionState = DescriptionState.None,
        val whatsNextState: WhatsNextState = WhatsNextState.None,
    ) : TripDetailUiState()

    /** An error occurred (e.g. trip not found). */
    data object Error : TripDetailUiState()
}

/** Represents the state of the on-device AI trip description shown in the Details tab. */
sealed class DescriptionState {
    /** Description generation is in progress (model may be downloading or generating). */
    data object Loading : DescriptionState()

    /** Description was generated successfully. */
    data class Available(val text: String) : DescriptionState()

    /** Gemini Nano is not supported on this device. */
    data object Unavailable : DescriptionState()

    /** An error occurred during description generation. */
    data object Error : DescriptionState()

    /** The user deleted the description. */
    data object None : DescriptionState()
}

/** Represents the state of the AI-generated "what's next" notice shown in the Details tab. */
sealed class WhatsNextState {
    /** Notice generation is in progress (model may be downloading or generating). */
    data object Loading : WhatsNextState()

    /** Notice was generated successfully. */
    data class Available(val text: String) : WhatsNextState()

    /** Gemini Nano is not supported on this device. */
    data object Unavailable : WhatsNextState()

    /** An error occurred during notice generation. */
    data object Error : WhatsNextState()

    /** Notice has not been generated yet. */
    data object None : WhatsNextState()
}
