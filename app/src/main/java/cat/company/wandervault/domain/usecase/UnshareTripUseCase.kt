package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripSyncRepository

/** Use-case that removes a shared trip from Firestore and clears sharing metadata locally. */
class UnshareTripUseCase(private val repository: TripSyncRepository) {
    suspend operator fun invoke(tripId: Int) = repository.unshareTrip(tripId)
}
