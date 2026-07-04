package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripChatRepository

class DeleteTripChatSessionUseCase(private val repository: TripChatRepository) {
    suspend operator fun invoke(sessionId: Int) = repository.deleteSession(sessionId)
}
