package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripChatRepository

class CreateTripChatSessionUseCase(private val repository: TripChatRepository) {
    suspend operator fun invoke(tripId: Int): Int = repository.createSession(tripId)
}
