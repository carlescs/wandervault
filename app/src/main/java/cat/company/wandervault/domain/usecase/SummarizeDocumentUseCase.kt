package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.repository.DocumentSummaryRepository

/**
 * Use-case that extracts a summary and trip-relevant information from a travel document using
 * on-device ML Kit.
 */
class SummarizeDocumentUseCase(private val repository: DocumentSummaryRepository) {
    suspend fun isAvailable(): Boolean = repository.isAvailable()
    suspend operator fun invoke(
        fileUri: String,
        mimeType: String,
        tripYear: Int? = null,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)? = null,
    ): DocumentExtractionResult? =
        repository.extractDocumentInfo(fileUri, mimeType, tripYear, onDownloadProgress)
}
