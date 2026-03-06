package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use-case that returns a [Flow] emitting the list of [Transport] legs used to travel to the given
 * destination (i.e. the transports of the preceding stop in the same trip), or an empty list if
 * the destination is the first in the trip or its predecessor has no transports assigned.
 */
class GetArrivalTransportForDestinationUseCase(private val repository: DestinationRepository) {
    operator fun invoke(destinationId: Int): Flow<List<Transport>> =
        repository.getArrivalTransportForDestination(destinationId)
}
