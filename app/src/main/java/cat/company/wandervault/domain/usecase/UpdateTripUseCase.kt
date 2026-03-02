package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository

class UpdateTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(trip: Trip) = repository.updateTrip(trip)
}
