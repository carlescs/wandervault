package cat.company.wandervault.ui.screens

/**
 * State of a filename suggestion request driven by on-device ML Kit.
 */
sealed class SuggestNameUiState {
    /** ML Kit analysis is running. */
    data object Loading : SuggestNameUiState()

    /**
     * The Gemini Nano model weights are being downloaded before the suggestion can be generated.
     *
     * @param bytesDownloaded Total bytes of model data downloaded so far.
     */
    data class Downloading(val bytesDownloaded: Long) : SuggestNameUiState()

    /**
     * A filename suggestion was produced successfully.
     *
     * @param suggestedName The suggested filename (without file extension).
     */
    data class Success(val suggestedName: String) : SuggestNameUiState()

    /**
     * AI filename suggestion is not available on this device or the document content could
     * not be read. This is a permanent state — retrying will not help.
     */
    data object Unavailable : SuggestNameUiState()

    /**
     * The suggestion failed with a transient error. The user may try again.
     *
     * @param message Optional error detail that can be shown in the UI.
     */
    data class Error(val message: String? = null) : SuggestNameUiState()
}
