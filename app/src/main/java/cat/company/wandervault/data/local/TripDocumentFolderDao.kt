package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDocumentFolderDao {
    @Insert
    suspend fun insert(folder: TripDocumentFolderEntity)

    @Update
    suspend fun update(folder: TripDocumentFolderEntity)

    @Delete
    suspend fun delete(folder: TripDocumentFolderEntity)

    /** Returns root folders (no parent) for the given trip, ordered by name. */
    @Query("SELECT * FROM trip_document_folders WHERE tripId = :tripId AND parentFolderId IS NULL ORDER BY name ASC")
    fun getRootFolders(tripId: Int): Flow<List<TripDocumentFolderEntity>>

    /** Returns direct child folders of [parentFolderId], ordered by name. */
    @Query("SELECT * FROM trip_document_folders WHERE parentFolderId = :parentFolderId ORDER BY name ASC")
    fun getSubFolders(parentFolderId: Int): Flow<List<TripDocumentFolderEntity>>

    /**
     * Returns the count of root-level folders in [tripId] with the given [name],
     * excluding the folder with [excludeId] (used for rename checks).
     */
    @Query(
        "SELECT COUNT(*) FROM trip_document_folders " +
            "WHERE tripId = :tripId AND parentFolderId IS NULL AND name = :name AND id != :excludeId",
    )
    suspend fun countRootFoldersByName(tripId: Int, name: String, excludeId: Int = 0): Int

    /** Returns all folders belonging to [tripId], ordered by name. */
    @Query("SELECT * FROM trip_document_folders WHERE tripId = :tripId ORDER BY name ASC")
    fun getAllByTripId(tripId: Int): Flow<List<TripDocumentFolderEntity>>
}
