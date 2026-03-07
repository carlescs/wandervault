package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Trip

/** UI state for the Share / "Save to Trip" screen. */
sealed class ShareUiState {

    /** Processing the incoming shared content (copying file, extracting text). */
    data object Processing : ShareUiState()

    /**
     * Content has been processed and is ready for the user to review and save.
     *
     * @param fileName Display name of the shared file or text snippet.
     * @param mimeType MIME type of the shared content.
     * @param localUri Internal URI where the file copy is stored (null for plain text snippets).
     * @param extractedText Text extracted from the content (via ML Kit or direct for text types).
     * @param trips List of available trips the user can associate the document with.
     * @param selectedTripId Currently selected trip ID, or null if none selected yet.
     * @param folder Folder name entered by the user.
     */
    data class Ready(
        val fileName: String,
        val mimeType: String,
        val localUri: String?,
        val extractedText: String?,
        val trips: List<Trip>,
        val selectedTripId: Int?,
        val folder: String,
    ) : ShareUiState()

    /** The document was saved successfully. */
    data object Saved : ShareUiState()

    /** An error occurred while processing the shared content. */
    data class Error(val message: String) : ShareUiState()
}
