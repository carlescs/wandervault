package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use-case that returns a [Flow] emitting the [Destination] that immediately follows the given
 * destination in the trip itinerary (i.e. the destination at position + 1 in the same trip),
 * or `null` if the given destination is the last in the trip.
 *
 * @param repository Repository used to look up destinations by trip and position.
 */
class GetNextDestinationUseCase(private val repository: DestinationRepository) {
    /**
     * Returns a [Flow] emitting the destination that immediately follows [currentDestination]
     * in the same trip itinerary, or `null` if [currentDestination] is the last destination.
     *
     * @param currentDestination The destination whose successor is requested.
     */
    operator fun invoke(currentDestination: Destination): Flow<Destination?> =
        repository.getDestinationByTripAndPosition(
            tripId = currentDestination.tripId,
            position = currentDestination.position + 1,
        )
}
