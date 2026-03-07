package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Insert
    suspend fun insert(trip: TripEntity)

    @Update
    suspend fun update(trip: TripEntity)

    @Delete
    suspend fun delete(trip: TripEntity)

    @Query("UPDATE trips SET isFavorite = 1 - isFavorite WHERE id = :tripId")
    suspend fun toggleFavorite(tripId: Int)

    @Query("SELECT * FROM trips WHERE isFavorite = 1 ORDER BY id ASC")
    fun getFavorites(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips ORDER BY id ASC")
    fun getAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun getById(id: Int): Flow<TripEntity?>
}
