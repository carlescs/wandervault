package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HotelDao {
    @Query("SELECT * FROM hotels WHERE destinationId = :destinationId")
    fun getByDestinationId(destinationId: Int): Flow<HotelEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(hotel: HotelEntity)

    @Update
    suspend fun update(hotel: HotelEntity)

    @Delete
    suspend fun delete(hotel: HotelEntity)
}
