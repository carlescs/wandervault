package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use-case that returns a [Flow] emitting the ordered list of [Destination]s for a given trip.
 */
class GetDestinationsForTripUseCase(private val repository: DestinationRepository) {
    operator fun invoke(tripId: Int): Flow<List<Destination>> =
        repository.getDestinationsForTrip(tripId)
}
