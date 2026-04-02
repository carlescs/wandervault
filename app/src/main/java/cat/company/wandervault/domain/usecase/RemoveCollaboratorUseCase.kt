package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripSyncRepository

/** Use-case that removes a collaborator from a shared trip.  Only the owner may call this. */
class RemoveCollaboratorUseCase(private val repository: TripSyncRepository) {
    suspend operator fun invoke(shareId: String, collaboratorUid: String) =
        repository.removeCollaborator(shareId, collaboratorUid)
}
