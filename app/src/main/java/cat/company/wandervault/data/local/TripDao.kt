package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity)

    @Query("SELECT * FROM trips ORDER BY startDate ASC")
    fun getAll(): Flow<List<TripEntity>>
}
