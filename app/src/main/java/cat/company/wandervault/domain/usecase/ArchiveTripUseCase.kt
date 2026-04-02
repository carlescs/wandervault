package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripRepository

/** Use-case that archives a trip, hiding it from the main trip list. */
class ArchiveTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(tripId: Int) = repository.archiveTrip(tripId)
}
