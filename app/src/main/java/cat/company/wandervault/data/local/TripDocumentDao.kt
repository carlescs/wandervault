package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDocumentDao {
    @Insert
    suspend fun insert(document: TripDocumentEntity)

    @Update
    suspend fun update(document: TripDocumentEntity)

    @Delete
    suspend fun delete(document: TripDocumentEntity)

    /** Returns all documents inside [folderId], ordered by name. */
    @Query("SELECT * FROM trip_documents WHERE folderId = :folderId ORDER BY name ASC")
    fun getByFolderId(folderId: Int): Flow<List<TripDocumentEntity>>

    /** Returns all root-level documents (folderId IS NULL) for [tripId], ordered by name. */
    @Query("SELECT * FROM trip_documents WHERE tripId = :tripId AND folderId IS NULL ORDER BY name ASC")
    fun getRootDocuments(tripId: Int): Flow<List<TripDocumentEntity>>

    /**
     * Returns the number of root-level documents with [name] for [tripId], excluding [excludeId].
     * Used for manual uniqueness enforcement since SQLite unique indexes treat NULLs as distinct.
     */
    @Query(
        "SELECT COUNT(*) FROM trip_documents WHERE tripId = :tripId AND folderId IS NULL AND name = :name AND id != :excludeId",
    )
    suspend fun countRootDocumentsByName(tripId: Int, name: String, excludeId: Int = 0): Int

    /**
     * Returns the URI strings of all documents whose [folderId] is in the entire sub-tree rooted at
     * [rootFolderId], including documents directly inside [rootFolderId] (the base case of the
     * recursive CTE). Uses a recursive CTE to walk the folder hierarchy so that orphaned internal
     * files can be deleted before the folder (and its DB descendants) are cascade-deleted.
     */
    @Query(
        """
        WITH RECURSIVE folder_tree(id) AS (
            SELECT :rootFolderId
            UNION ALL
            SELECT f.id FROM trip_document_folders f
            INNER JOIN folder_tree ft ON f.parentFolderId = ft.id
        )
        SELECT uri FROM trip_documents WHERE folderId IN (SELECT id FROM folder_tree)
        """,
    )
    suspend fun getDocumentUrisInFolderTree(rootFolderId: Int): List<String>

    /** Returns the [uri] strings for every document belonging to [tripId] (all folders + root). */
    @Query("SELECT uri FROM trip_documents WHERE tripId = :tripId")
    suspend fun getAllDocumentUrisByTripId(tripId: Int): List<String>

    /** Returns a [Flow] emitting the document with the given [id], or `null` if not found. */
    @Query("SELECT * FROM trip_documents WHERE id = :id")
    fun getById(id: Int): Flow<TripDocumentEntity?>
}
