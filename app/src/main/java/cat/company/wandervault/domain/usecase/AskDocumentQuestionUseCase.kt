package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.DocumentSummaryRepository

/**
 * Use-case that asks on-device Gemini Nano a free-form question about a travel document.
 */
class AskDocumentQuestionUseCase(private val repository: DocumentSummaryRepository) {
    suspend fun isAvailable(): Boolean = repository.isAvailable()
    suspend operator fun invoke(
        fileUri: String,
        mimeType: String,
        question: String,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)? = null,
    ): String? = repository.askQuestion(fileUri, mimeType, question, onDownloadProgress)
}
