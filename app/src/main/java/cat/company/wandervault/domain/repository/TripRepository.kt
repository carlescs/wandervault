package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Trip
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

interface TripRepository {
    fun getTrips(): Flow<List<Trip>>
    fun getFavoriteTrips(): Flow<List<Trip>>

    /**
     * Returns a [Flow] that emits the trip with the given [id], or `null` if not found.
     */
    fun getTripById(id: Int): Flow<Trip?>
    suspend fun saveTrip(trip: Trip)
    suspend fun updateTrip(trip: Trip)
    suspend fun toggleFavoriteTrip(tripId: Int)
    suspend fun deleteTrip(trip: Trip)

    /**
     * Partially updates only the [nextStep] and [nextStepDeadline] fields of the trip with the
     * given [tripId].  All other columns are left unchanged, avoiding accidental overwrites of
     * concurrent user edits.
     */
    suspend fun updateTripWhatsNext(tripId: Int, nextStep: String?, nextStepDeadline: ZonedDateTime?)
}
