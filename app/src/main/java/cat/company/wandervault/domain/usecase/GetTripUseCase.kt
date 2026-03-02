package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use-case that returns a [Flow] emitting the [Trip] with the given [id],
 * or `null` if no such trip exists.
 */
class GetTripUseCase(private val repository: TripRepository) {
    operator fun invoke(id: Int): Flow<Trip?> = repository.getTripById(id)
}
