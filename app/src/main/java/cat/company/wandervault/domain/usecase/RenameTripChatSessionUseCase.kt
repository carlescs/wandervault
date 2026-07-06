package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripChatRepository

/** Renames a saved chat session, or clears the name when [name] is `null`. */
class RenameTripChatSessionUseCase(private val repository: TripChatRepository) {
    suspend operator fun invoke(sessionId: Int, name: String?) =
        repository.renameSession(sessionId, name)
}
