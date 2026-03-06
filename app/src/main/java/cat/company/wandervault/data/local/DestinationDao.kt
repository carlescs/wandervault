package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DestinationDao {
    @Insert
    suspend fun insert(destination: DestinationEntity)

    @Update
    suspend fun update(destination: DestinationEntity)

    @Delete
    suspend fun delete(destination: DestinationEntity)

    @Query("SELECT * FROM destinations WHERE id = :id")
    fun getById(id: Int): Flow<DestinationEntity?>

    @Query("SELECT * FROM destinations WHERE tripId = :tripId AND position = :position LIMIT 1")
    fun getByTripIdAndPosition(tripId: Int, position: Int): Flow<DestinationEntity?>

    @Query("SELECT * FROM destinations WHERE tripId = :tripId ORDER BY position ASC")
    fun getByTripId(tripId: Int): Flow<List<DestinationEntity>>

    @Query("SELECT tripId, arrivalDateTime, departureDateTime FROM destinations WHERE tripId = :tripId")
    fun getDateProjectionsForTrip(tripId: Int): Flow<List<DestinationDateProjection>>

    @Query("SELECT tripId, arrivalDateTime, departureDateTime FROM destinations")
    fun getAll(): Flow<List<DestinationDateProjection>>
}
