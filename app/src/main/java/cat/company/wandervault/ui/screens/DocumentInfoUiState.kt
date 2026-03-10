package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.TripDocument

/**
 * UI state for the Document Info screen.
 */
sealed class DocumentInfoUiState {
    /** Data is being loaded. */
    data object Loading : DocumentInfoUiState()

    /**
     * Document data loaded successfully.
     *
     * @param document The document whose information is displayed.
     * @param fileSizeBytes The size of the document file in bytes, or `null` if unavailable.
     * @param folderName The name of the folder this document belongs to, or `null` for root-level documents.
     */
    data class Success(
        val document: TripDocument,
        val fileSizeBytes: Long? = null,
        val folderName: String? = null,
    ) : DocumentInfoUiState()

    /** The requested document could not be found. */
    data object NotFound : DocumentInfoUiState()
}
