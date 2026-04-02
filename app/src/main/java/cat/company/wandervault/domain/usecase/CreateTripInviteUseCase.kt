package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripSyncRepository

/**
 * Use-case that creates a short invite code for a shared trip.
 *
 * @return A 6-character alphanumeric invite code.
 */
class CreateTripInviteUseCase(private val repository: TripSyncRepository) {
    suspend operator fun invoke(shareId: String): String = repository.createInviteCode(shareId)
}
