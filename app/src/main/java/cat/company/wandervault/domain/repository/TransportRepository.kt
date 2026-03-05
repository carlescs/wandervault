package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Transport

/** Repository for managing [Transport] entities. */
interface TransportRepository {
    /** Persists a new [transport] to the data store. */
    suspend fun saveTransport(transport: Transport)

    /** Updates an existing [transport] in the data store. */
    suspend fun updateTransport(transport: Transport)

    /** Removes [transport] from the data store. */
    suspend fun deleteTransport(transport: Transport)
}
