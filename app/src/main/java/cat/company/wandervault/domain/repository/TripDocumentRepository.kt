package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import kotlinx.coroutines.flow.Flow

/** Repository for managing [TripDocumentFolder] and [TripDocument] data. */
interface TripDocumentRepository {
    /** Returns a [Flow] emitting root-level folders for the given [tripId], ordered by name. */
    fun getRootFolders(tripId: Int): Flow<List<TripDocumentFolder>>

    /** Returns a [Flow] emitting direct child folders of [parentFolderId], ordered by name. */
    fun getSubFolders(parentFolderId: Int): Flow<List<TripDocumentFolder>>

    /** Returns a [Flow] emitting documents inside [folderId], ordered by name. */
    fun getDocumentsInFolder(folderId: Int): Flow<List<TripDocument>>

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
}
