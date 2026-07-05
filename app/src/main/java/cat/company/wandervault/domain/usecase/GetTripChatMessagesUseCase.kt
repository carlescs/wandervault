package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripChatMessage
import cat.company.wandervault.domain.repository.TripChatRepository
import kotlinx.coroutines.flow.Flow

class GetTripChatMessagesUseCase(private val repository: TripChatRepository) {
    operator fun invoke(sessionId: Int): Flow<List<TripChatMessage>> =
        repository.getMessagesForSession(sessionId)
}
