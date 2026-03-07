package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.TripDocument
import kotlinx.coroutines.flow.Flow

/**
 * Repository for documents attached to trips.
 */
interface TripDocumentRepository {

    /** Returns a live [Flow] of all documents for the given [tripId], ordered by creation date. */
    fun getDocumentsForTrip(tripId: Int): Flow<List<TripDocument>>

    /** Persists a new [document] and returns its auto-generated ID. */
    suspend fun saveDocument(document: TripDocument): Long

    /** Deletes the given [document] and removes its file from internal storage. */
    suspend fun deleteDocument(document: TripDocument)
}
