package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.repository.DestinationRepository

/** Use-case that persists a new [Destination] to the repository. */
class SaveDestinationUseCase(private val repository: DestinationRepository) {
    suspend operator fun invoke(destination: Destination) =
        repository.saveDestination(destination)
}
