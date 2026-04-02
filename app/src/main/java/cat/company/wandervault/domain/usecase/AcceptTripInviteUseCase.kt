package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripSyncRepository

/**
 * Use-case that validates an invite code, adds the current user to the trip's collaborators, and
 * returns the share ID so [JoinTripUseCase] can be called.
 */
class AcceptTripInviteUseCase(private val repository: TripSyncRepository) {
    suspend operator fun invoke(inviteCode: String): String = repository.acceptInviteCode(inviteCode)
}
