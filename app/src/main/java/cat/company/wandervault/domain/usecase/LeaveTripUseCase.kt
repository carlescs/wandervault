package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripSyncRepository

/** Use-case that removes the current user from a shared trip's collaborators and deletes the local copy. */
class LeaveTripUseCase(private val repository: TripSyncRepository) {
    suspend operator fun invoke(shareId: String) = repository.leaveTrip(shareId)
}
