package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Destination
import kotlinx.coroutines.flow.Flow

/** Repository for managing [Destination] data. */
interface DestinationRepository {
    /** Returns a [Flow] that emits the ordered list of destinations for the given trip. */
    fun getDestinationsForTrip(tripId: Int): Flow<List<Destination>>

    /** Persists a new [destination] to the data store. */
    suspend fun saveDestination(destination: Destination)

    /** Updates an existing [destination] in the data store. */
    suspend fun updateDestination(destination: Destination)

    /** Removes [destination] from the data store. */
    suspend fun deleteDestination(destination: Destination)
}
