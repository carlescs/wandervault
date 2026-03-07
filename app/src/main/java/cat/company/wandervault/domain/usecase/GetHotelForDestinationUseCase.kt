package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.repository.HotelRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use-case that returns a [Flow] emitting the [Hotel] for the given [destinationId],
 * or `null` if no hotel has been set for that destination.
 */
class GetHotelForDestinationUseCase(private val repository: HotelRepository) {
    operator fun invoke(destinationId: Int): Flow<Hotel?> =
        repository.getHotelForDestination(destinationId)
}
