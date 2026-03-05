package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Hotel
import kotlinx.coroutines.flow.Flow

/** Repository for managing [Hotel] entities. */
interface HotelRepository {
    /** Returns a [Flow] emitting the hotel for the given [destinationId], or null if none exists. */
    fun getHotelForDestination(destinationId: Int): Flow<Hotel?>

    /** Persists a new [hotel] to the data store. */
    suspend fun saveHotel(hotel: Hotel)

    /** Updates an existing [hotel] in the data store. */
    suspend fun updateHotel(hotel: Hotel)

    /** Removes [hotel] from the data store. */
    suspend fun deleteHotel(hotel: Hotel)
}
