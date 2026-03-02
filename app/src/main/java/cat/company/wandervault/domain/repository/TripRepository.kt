package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Trip
import kotlinx.coroutines.flow.Flow

interface TripRepository {
    fun getTrips(): Flow<List<Trip>>
    suspend fun saveTrip(trip: Trip)
}
