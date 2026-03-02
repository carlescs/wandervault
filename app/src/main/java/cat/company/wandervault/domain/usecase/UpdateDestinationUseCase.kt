package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.repository.DestinationRepository

/** Use-case that updates an existing [Destination] in the repository. */
class UpdateDestinationUseCase(private val repository: DestinationRepository) {
    suspend operator fun invoke(destination: Destination) =
        repository.updateDestination(destination)
}
