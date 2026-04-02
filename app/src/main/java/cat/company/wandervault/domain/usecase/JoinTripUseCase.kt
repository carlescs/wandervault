package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripSyncRepository

/**
 * Use-case that joins a shared trip identified by a share ID, downloading it from Firestore and
 * saving it locally.
 *
 * @return The local [Trip] that was created.
 */
class JoinTripUseCase(private val repository: TripSyncRepository) {
    suspend operator fun invoke(shareId: String): Trip = repository.joinTrip(shareId)
}
