package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder

/**
 * Transient error type for failed write operations in the Documents screen.
 * The Screen maps each variant to the appropriate string resource.
 */
enum class DocumentsWriteError {
    /** A folder or document with the requested name already exists in this scope. */
    DuplicateName,
    /** A generic, unclassified write failure. */
    Generic,
}

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
     * @param documents Documents in the current folder (including root-level documents when [currentFolder] is `null`).
     * @param currentFolder The folder currently being viewed, or `null` when at the root level.
     * @param folderStack Navigation path of folders from the root to the current folder (last element = current folder).
     * @param allFolders All folders in the trip regardless of depth, used for the move-document picker.
     * @param allDocuments All documents in the trip regardless of folder, used to decide whether
     *   auto-organize is available (the AI analyzes all documents when at root level).
     * @param writeError A one-off error from a failed write operation (create/rename/delete),
     *   or `null` when there is no pending error. Call [TripDocumentsViewModel.clearError] to dismiss.
     * @param selectedDocumentIds IDs of documents currently selected in multi-select mode.
     *   Empty when no selection is active. Call [TripDocumentsViewModel.clearSelection] to exit selection mode.
     */
    data class Success(
        val folders: List<TripDocumentFolder> = emptyList(),
        val documents: List<TripDocument> = emptyList(),
        val currentFolder: TripDocumentFolder? = null,
        val folderStack: List<TripDocumentFolder> = emptyList(),
        val allFolders: List<TripDocumentFolder> = emptyList(),
        val allDocuments: List<TripDocument> = emptyList(),
        val writeError: DocumentsWriteError? = null,
        val selectedDocumentIds: Set<Int> = emptySet(),
        /** Whether on-device AI is supported on this device. */
        val isAiAvailable: Boolean = true,
        /**
         * Current state of an in-flight or completed auto-organize request, or `null` when no
         * auto-organize is active. Call [TripDocumentsViewModel.cancelAutoOrganize] to dismiss.
         */
        val autoOrganizeState: AutoOrganizeUiState? = null,
    ) : TripDocumentsUiState()
}
