package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import kotlinx.coroutines.flow.Flow

/** Repository for managing [TripDocumentFolder] and [TripDocument] data. */
interface TripDocumentRepository {
    /** Returns a [Flow] emitting the [TripDocument] with the given [id], or `null` if not found. */
    fun getDocumentById(id: Int): Flow<TripDocument?>

    /** Returns a [Flow] emitting root-level folders for the given [tripId], ordered by name. */
    fun getRootFolders(tripId: Int): Flow<List<TripDocumentFolder>>

    /** Returns a [Flow] emitting direct child folders of [parentFolderId], ordered by name. */
    fun getSubFolders(parentFolderId: Int): Flow<List<TripDocumentFolder>>

    /** Returns a [Flow] emitting documents inside [folderId], ordered by name. */
    fun getDocumentsInFolder(folderId: Int): Flow<List<TripDocument>>

    /** Returns a [Flow] emitting root-level documents (no folder) for [tripId], ordered by name. */
    fun getRootDocuments(tripId: Int): Flow<List<TripDocument>>

    /** Returns a [Flow] emitting all folders (at any depth) for [tripId], ordered by name. */
    fun getAllFoldersForTrip(tripId: Int): Flow<List<TripDocumentFolder>>

    /** Returns a [Flow] emitting all documents for [tripId] (all folders + root), ordered by name. */
    fun getAllDocumentsForTrip(tripId: Int): Flow<List<TripDocument>>

    /** Persists a new [folder]. */
    suspend fun saveFolder(folder: TripDocumentFolder)

    /** Updates an existing [folder]. */
    suspend fun updateFolder(folder: TripDocumentFolder)

    /** Removes a [folder] and all its contents (cascade). */
    suspend fun deleteFolder(folder: TripDocumentFolder)

    /** Persists a new [document]. */
    suspend fun saveDocument(document: TripDocument)

    /** Updates an existing [document]. */
    suspend fun updateDocument(document: TripDocument)

    /** Removes a [document] from the data store. */
    suspend fun deleteDocument(document: TripDocument)

    /**
     * Copies the file at [sourceUri] to the app's internal documents directory.
     * @return The internal file URI string, or `null` if the copy fails.
     */
    suspend fun copyDocumentToInternalStorage(sourceUri: String): String?

    /**
     * Returns the internal-storage URI strings for every document belonging to [tripId].
     * Includes documents at root level and inside any folder. Intended for use before a trip is
     * deleted so that physical files can be cleaned up after the DB cascade removes the rows.
     */
    suspend fun getAllDocumentUrisForTrip(tripId: Int): List<String>

    /**
     * Deletes the physical file at [fileUri] from internal document storage (best-effort).
     * Only files inside the app's `filesDir/documents/` directory are affected; external URIs
     * are silently ignored. Errors (missing file, I/O failure) are also silently ignored.
     */
    suspend fun deleteDocumentFileByUri(fileUri: String)
}
