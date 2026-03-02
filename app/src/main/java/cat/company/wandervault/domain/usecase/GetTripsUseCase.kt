package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow

class GetTripsUseCase(private val repository: TripRepository) {
    operator fun invoke(): Flow<List<Trip>> = repository.getTrips()
}
