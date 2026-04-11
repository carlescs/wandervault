package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE destinationId = :destinationId ORDER BY dateTime IS NULL ASC, dateTime ASC, id ASC")
    fun getByDestinationId(destinationId: Int): Flow<List<ActivityEntity>>

    @Query("SELECT a.* FROM activities a INNER JOIN destinations d ON a.destinationId = d.id WHERE d.tripId = :tripId ORDER BY a.dateTime IS NULL ASC, a.dateTime ASC, a.id ASC")
    fun getByTripId(tripId: Int): Flow<List<ActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(activity: ActivityEntity)

    @Update
    suspend fun update(activity: ActivityEntity)

    @Delete
    suspend fun delete(activity: ActivityEntity)
}
