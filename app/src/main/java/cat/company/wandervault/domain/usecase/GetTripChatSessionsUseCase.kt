package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripChatSession
import cat.company.wandervault.domain.repository.TripChatRepository
import kotlinx.coroutines.flow.Flow

class GetTripChatSessionsUseCase(private val repository: TripChatRepository) {
    operator fun invoke(tripId: Int): Flow<List<TripChatSession>> =
        repository.getSessionsForTrip(tripId)
}
