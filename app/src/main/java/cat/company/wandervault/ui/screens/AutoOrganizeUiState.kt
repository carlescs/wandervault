package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.OrganizationPlan

/**
 * State of an in-progress or completed AI auto-organize request for the Documents screen.
 */
sealed class AutoOrganizeUiState {
    /** AI analysis is running. */
    data object Loading : AutoOrganizeUiState()

    /**
     * The Gemini Nano model weights are being downloaded before the plan can be generated.
     *
     * @param bytesDownloaded Total bytes of model data downloaded so far.
     */
    data class Downloading(val bytesDownloaded: Long) : AutoOrganizeUiState()

    /**
     * The organization plan was generated successfully. The user must confirm or cancel.
     *
     * @param plan The AI-suggested folder structure.
     */
    data class ReadyToConfirm(val plan: OrganizationPlan) : AutoOrganizeUiState()

    /**
     * AI auto-organize is not available on this device.
     * This is a permanent state — retrying will not help.
     */
    data object Unavailable : AutoOrganizeUiState()

    /**
     * The request failed with a transient error. The user may try again.
     *
     * @param message Optional error detail that can be shown in the UI.
     */
    data class Error(val message: String? = null) : AutoOrganizeUiState()
}
