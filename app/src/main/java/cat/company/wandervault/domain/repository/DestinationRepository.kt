package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Transport
import kotlinx.coroutines.flow.Flow

/** Repository for managing [Destination] data. */
interface DestinationRepository {
    /** Returns a [Flow] emitting the [Destination] with the given [id], or `null` if not found. */
    fun getDestinationById(id: Int): Flow<Destination?>

    /** Returns a [Flow] that emits the ordered list of destinations for the given trip. */
    fun getDestinationsForTrip(tripId: Int): Flow<List<Destination>>

    /**
     * Returns a [Flow] emitting the [Transport] (with its legs) used to travel to the destination
     * with the given [destinationId] (i.e. the transport of the preceding destination in the same
     * trip), or `null` if there is no preceding destination or no transport assigned to it.
     */
    fun getArrivalTransportForDestination(destinationId: Int): Flow<Transport?>

    /** Persists a new [destination] to the data store. */
    suspend fun saveDestination(destination: Destination)

    /** Updates an existing [destination] in the data store. */
    suspend fun updateDestination(destination: Destination)

    /** Removes [destination] from the data store. */
    suspend fun deleteDestination(destination: Destination)
}
