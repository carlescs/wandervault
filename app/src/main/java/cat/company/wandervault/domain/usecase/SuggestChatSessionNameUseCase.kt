package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripDescriptionRepository

/**
 * Use-case that uses on-device Gemini Nano to suggest a short name for a chat session
 * based on its conversation messages.
 */
class SuggestChatSessionNameUseCase(private val repository: TripDescriptionRepository) {
    suspend fun isAvailable(): Boolean = repository.isAvailable()

    suspend operator fun invoke(
        messages: List<String>,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)? = null,
    ): String? = repository.suggestChatSessionName(messages, onDownloadProgress)
}
