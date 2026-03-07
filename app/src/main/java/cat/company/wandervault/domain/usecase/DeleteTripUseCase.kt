package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository

/** Use-case that permanently removes a [Trip] and all its associated data from the repository. */
class DeleteTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(trip: Trip) = repository.deleteTrip(trip)
}
