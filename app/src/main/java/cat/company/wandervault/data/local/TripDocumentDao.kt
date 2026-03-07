package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDocumentDao {
    @Query("SELECT * FROM trip_documents WHERE tripId = :tripId ORDER BY createdAt ASC")
    fun getByTripId(tripId: Int): Flow<List<TripDocumentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(document: TripDocumentEntity): Long

    @Delete
    suspend fun delete(document: TripDocumentEntity)
}
