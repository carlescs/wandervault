package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder

/**
 * UI state for the Trip Documents screen.
 *
 * Represents the content of the currently visible folder (either the root level or a sub-folder).
 */
sealed class TripDocumentsUiState {
    /** Data is being loaded. */
    data object Loading : TripDocumentsUiState()

    /**
     * Data loaded successfully.
     *
     * @param folders Sub-folders of the current level (root folders or sub-folders of [currentFolder]).
     * @param documents Documents in the current folder (empty when at root level, as root has no documents directly).
     * @param currentFolder The folder currently being viewed, or `null` when at the root level.
     * @param folderStack Stack of ancestor folders used for back-navigation (top = immediate parent).
     */
    data class Success(
        val folders: List<TripDocumentFolder> = emptyList(),
        val documents: List<TripDocument> = emptyList(),
        val currentFolder: TripDocumentFolder? = null,
        val folderStack: List<TripDocumentFolder> = emptyList(),
    ) : TripDocumentsUiState()
}
