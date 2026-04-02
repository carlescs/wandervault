package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripSyncRepository

/**
 * Use-case that uploads a local trip to Firestore so it can be shared with collaborators.
 *
 * @return The newly generated share ID.
 */
class ShareTripUseCase(private val repository: TripSyncRepository) {
    suspend operator fun invoke(tripId: Int): String = repository.shareTrip(tripId)
}
