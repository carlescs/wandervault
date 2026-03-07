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
}
