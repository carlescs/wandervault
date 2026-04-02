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

    @Query(
        "UPDATE trips SET nextStep = :nextStep, nextStepDeadline = :nextStepDeadline WHERE id = :tripId",
    )
    suspend fun updateWhatsNext(tripId: Int, nextStep: String?, nextStepDeadline: String?)

    @Query("UPDATE trips SET isArchived = :isArchived WHERE id = :tripId")
    suspend fun setArchived(tripId: Int, isArchived: Boolean)

    @Query(
        "UPDATE trips SET shareId = :shareId, ownerId = :ownerId, collaboratorIds = :collaboratorIds WHERE id = :tripId",
    )
    suspend fun updateShareInfo(tripId: Int, shareId: String?, ownerId: String?, collaboratorIds: String)

    @Query("SELECT * FROM trips WHERE isFavorite = 1 AND isArchived = 0 ORDER BY id ASC")
    fun getFavorites(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE isArchived = 0 ORDER BY id ASC")
    fun getAll(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE isArchived = 1 ORDER BY id ASC")
    fun getArchived(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE id = :id")
    fun getById(id: Int): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE shareId = :shareId LIMIT 1")
    suspend fun getByShareId(shareId: String): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Int): TripEntity?

    @Query("SELECT * FROM trips")
    suspend fun getAllOnce(): List<TripEntity>
}
