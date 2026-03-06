package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use-case that returns a [Flow] emitting the [Transport] used to travel to the given destination
 * (i.e. the transport of the preceding stop in the same trip), or `null` if the destination is
 * the first in the trip or has no transport assigned to its predecessor.
 */
class GetArrivalTransportForDestinationUseCase(private val repository: DestinationRepository) {
    operator fun invoke(destinationId: Int): Flow<Transport?> =
        repository.getArrivalTransportForDestination(destinationId)
}
