package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TransportRepository

/**
 * Use-case that returns the ID of the [cat.company.wandervault.domain.model.Transport] for the
 * given destination, creating a new parent transport record if none exists yet.
 */
class GetOrCreateTransportForDestinationUseCase(private val repository: TransportRepository) {
    suspend operator fun invoke(destinationId: Int): Int =
        repository.getOrCreateTransportForDestination(destinationId)
}
