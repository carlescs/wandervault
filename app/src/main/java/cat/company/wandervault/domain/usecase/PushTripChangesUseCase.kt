package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripSyncRepository

/** Use-case that pushes local trip changes to Firestore. */
class PushTripChangesUseCase(private val repository: TripSyncRepository) {
    suspend operator fun invoke(tripId: Int) = repository.pushLocalChanges(tripId)
}
