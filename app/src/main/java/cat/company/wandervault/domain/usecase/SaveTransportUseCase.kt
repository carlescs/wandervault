package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TransportRepository

/**
 * Use-case that gets or creates the transport parent for the given destination and returns its ID.
 *
 * This use-case is kept for backward API compatibility.  Prefer
 * [GetOrCreateTransportForDestinationUseCase] in new code.
 */
@Deprecated("Use GetOrCreateTransportForDestinationUseCase instead")
class SaveTransportUseCase(private val repository: TransportRepository) {
    suspend operator fun invoke(destinationId: Int): Int =
        repository.getOrCreateTransportForDestination(destinationId)
}
