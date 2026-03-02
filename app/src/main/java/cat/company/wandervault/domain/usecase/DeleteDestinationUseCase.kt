package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.repository.DestinationRepository

/** Use-case that deletes a [Destination] from the repository. */
class DeleteDestinationUseCase(private val repository: DestinationRepository) {
    suspend operator fun invoke(destination: Destination) =
        repository.deleteDestination(destination)
}
