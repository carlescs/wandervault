package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.repository.HotelRepository

/**
 * Use-case that fetches hotels for all given destination IDs in a single query.
 * Returns a [Map] keyed by destination ID, containing only destinations that have a hotel.
 */
class GetHotelsForDestinationsUseCase(private val repository: HotelRepository) {
    suspend operator fun invoke(destinationIds: List<Int>): Map<Int, Hotel> =
        repository.getHotelsForDestinations(destinationIds)
}
