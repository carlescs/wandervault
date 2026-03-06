package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransportDao {
    @Insert
    suspend fun insert(transport: TransportEntity)

    @Update
    suspend fun update(transport: TransportEntity)

    @Delete
    suspend fun delete(transport: TransportEntity)

    @Query("SELECT * FROM transports WHERE destinationId = :destinationId ORDER BY position ASC")
    fun getByDestinationId(destinationId: Int): Flow<List<TransportEntity>>

    @Query(
        "SELECT t.* FROM transports t " +
            "INNER JOIN destinations d ON t.destinationId = d.id " +
            "WHERE d.tripId = :tripId " +
            "ORDER BY t.position ASC",
    )
    fun getByTripId(tripId: Int): Flow<List<TransportEntity>>

    @Query(
        "SELECT t.* FROM transports t " +
            "INNER JOIN destinations prev ON t.destinationId = prev.id " +
            "INNER JOIN destinations curr ON curr.id = :destinationId " +
            "WHERE prev.tripId = curr.tripId " +
            "AND prev.position = curr.position - 1 " +
            "ORDER BY t.position ASC",
    )
    fun getArrivalTransportForDestination(destinationId: Int): Flow<List<TransportEntity>>
}
