package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripRepository

/** Use-case that unarchives a trip, restoring it to the main trip list. */
class UnarchiveTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(tripId: Int) = repository.unarchiveTrip(tripId)
}
