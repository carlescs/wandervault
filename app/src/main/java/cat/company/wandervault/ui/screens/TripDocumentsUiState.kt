package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.DocumentExtractionResult
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
 * State of an in-progress or completed document analysis, shown via [AnalyzeDocumentDialog].
 */
sealed class AnalyzeDocumentUiState {
    /** ML Kit analysis is running. */
    data object Loading : AnalyzeDocumentUiState()

    /**
     * Analysis completed successfully.
     *
     * @param document The document that was analyzed.
     * @param extractionResult The full extraction result from ML Kit.
     */
    data class Result(
        val document: TripDocument,
        val extractionResult: DocumentExtractionResult,
    ) : AnalyzeDocumentUiState()

    /** Analysis failed with an error. */
    data object Error : AnalyzeDocumentUiState()
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
     * @param writeError A one-off error from a failed write operation (create/rename/delete),
     *   or `null` when there is no pending error. Call [TripDocumentsViewModel.clearError] to dismiss.
     * @param analyzeState The current state of an in-progress or completed document analysis,
     *   or `null` when no analysis is active. Call [TripDocumentsViewModel.dismissAnalyze] to dismiss.
     */
    data class Success(
        val folders: List<TripDocumentFolder> = emptyList(),
        val documents: List<TripDocument> = emptyList(),
        val currentFolder: TripDocumentFolder? = null,
        val folderStack: List<TripDocumentFolder> = emptyList(),
        val allFolders: List<TripDocumentFolder> = emptyList(),
        val writeError: DocumentsWriteError? = null,
        val analyzeState: AnalyzeDocumentUiState? = null,
    ) : TripDocumentsUiState()
}
