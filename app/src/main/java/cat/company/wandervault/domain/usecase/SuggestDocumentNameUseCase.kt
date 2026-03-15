package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.DocumentSummaryRepository

/**
 * Use-case that uses on-device ML Kit to suggest a concise filename for a travel document
 * based on its content.
 */
class SuggestDocumentNameUseCase(private val repository: DocumentSummaryRepository) {
    suspend fun isAvailable(): Boolean = repository.isAvailable()
    suspend operator fun invoke(
        fileUri: String,
        mimeType: String,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)? = null,
    ): String? = repository.suggestDocumentName(fileUri, mimeType, onDownloadProgress)
}
