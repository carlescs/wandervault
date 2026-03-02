package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository

class SaveTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(trip: Trip) = repository.saveTrip(trip)
}
