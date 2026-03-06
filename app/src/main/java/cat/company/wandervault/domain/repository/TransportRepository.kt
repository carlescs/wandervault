package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.model.TransportLeg

/** Repository for managing [Transport] and [TransportLeg] entities. */
interface TransportRepository {
    /**
     * Returns the ID of the [Transport] for [destinationId], creating one if none exists.
     *
     * This is used when saving legs for the first time: the parent transport record is
     * created lazily so callers don't have to manage the parent lifecycle manually.
     */
    suspend fun getOrCreateTransportForDestination(destinationId: Int): Int

    /** Removes [transport] and all its [TransportLeg]s from the data store (via CASCADE). */
    suspend fun deleteTransport(transport: Transport)

    /** Persists a new [leg] to the data store. */
    suspend fun saveTransportLeg(leg: TransportLeg)

    /** Updates an existing [leg] in the data store. */
    suspend fun updateTransportLeg(leg: TransportLeg)

    /** Removes [leg] from the data store. */
    suspend fun deleteTransportLeg(leg: TransportLeg)
}
