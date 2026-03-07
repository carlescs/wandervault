package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Hotel
import kotlinx.coroutines.flow.Flow

/** Repository for managing [Hotel] data. */
interface HotelRepository {
    /** Returns a [Flow] emitting the [Hotel] for the given [destinationId], or `null` if not set. */
    fun getHotelForDestination(destinationId: Int): Flow<Hotel?>

    /** Persists or updates the [hotel] for a destination. */
    suspend fun saveHotel(hotel: Hotel)
}
