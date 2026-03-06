package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransportDao {
    @Insert
    suspend fun insert(transport: TransportEntity): Long

    @Delete
    suspend fun delete(transport: TransportEntity)

    /** Returns the transport parent for a destination, or `null` if none exists (reactive). */
    @Query("SELECT * FROM transports WHERE destinationId = :destinationId LIMIT 1")
    fun getByDestinationId(destinationId: Int): Flow<TransportEntity?>

    /** Returns the transport parent for a destination once (non-reactive, for use in suspending operations). */
    @Query("SELECT * FROM transports WHERE destinationId = :destinationId LIMIT 1")
    suspend fun getByDestinationIdOnce(destinationId: Int): TransportEntity?

    @Query(
        "SELECT t.* FROM transports t " +
            "INNER JOIN destinations d ON t.destinationId = d.id " +
            "WHERE d.tripId = :tripId",
    )
    fun getByTripId(tripId: Int): Flow<List<TransportEntity>>

    /** Returns the transport of the destination that immediately precedes this one in the same trip. */
    @Query(
        "SELECT t.* FROM transports t " +
            "INNER JOIN destinations prev ON t.destinationId = prev.id " +
            "INNER JOIN destinations curr ON curr.id = :destinationId " +
            "WHERE prev.tripId = curr.tripId " +
            "AND prev.position = curr.position - 1 " +
            "LIMIT 1",
    )
    fun getArrivalTransportForDestination(destinationId: Int): Flow<TransportEntity?>
}
