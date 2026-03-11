package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Hotel
import kotlinx.coroutines.flow.Flow

/** Repository for managing [Hotel] data. */
interface HotelRepository {
    /** Returns a [Flow] emitting the [Hotel] for the given [destinationId], or `null` if not set. */
    fun getHotelForDestination(destinationId: Int): Flow<Hotel?>

    /** Returns hotels for all given [destinationIds] in a single query, keyed by destination ID. */
    suspend fun getHotelsForDestinations(destinationIds: List<Int>): Map<Int, Hotel>

    /** Persists or updates the [hotel] for a destination. */
    suspend fun saveHotel(hotel: Hotel)

    /** Removes the [hotel] from the data store. */
    suspend fun deleteHotel(hotel: Hotel)
}
