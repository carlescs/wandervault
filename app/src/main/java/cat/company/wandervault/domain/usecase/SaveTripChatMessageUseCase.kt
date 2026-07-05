package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripChatMessageKind
import cat.company.wandervault.domain.repository.TripChatRepository

class SaveTripChatMessageUseCase(private val repository: TripChatRepository) {
    suspend operator fun invoke(sessionId: Int, kind: TripChatMessageKind, text: String?) {
        repository.saveMessage(sessionId = sessionId, kind = kind, text = text)
    }
}
