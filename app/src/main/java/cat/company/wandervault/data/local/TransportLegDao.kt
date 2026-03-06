package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransportLegDao {
    @Insert
    suspend fun insert(leg: TransportLegEntity)

    @Update
    suspend fun update(leg: TransportLegEntity)

    @Delete
    suspend fun delete(leg: TransportLegEntity)

    @Query("SELECT * FROM transport_legs WHERE transportId = :transportId ORDER BY position ASC")
    fun getByTransportId(transportId: Int): Flow<List<TransportLegEntity>>

    @Query(
        "SELECT tl.* FROM transport_legs tl " +
            "INNER JOIN transports t ON tl.transportId = t.id " +
            "INNER JOIN destinations d ON t.destinationId = d.id " +
            "WHERE d.tripId = :tripId " +
            "ORDER BY tl.position ASC",
    )
    fun getByTripId(tripId: Int): Flow<List<TransportLegEntity>>
}
