package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use-case that returns a [Flow] emitting the [Transport] (with its legs) used to travel to the
 * given destination (i.e. the transport of the preceding stop in the same trip), or `null` if the
 * destination is the first in the trip or its predecessor has no transport assigned.
 */
class GetArrivalTransportForDestinationUseCase(private val repository: DestinationRepository) {
    operator fun invoke(destinationId: Int): Flow<Transport?> =
        repository.getArrivalTransportForDestination(destinationId)
}
