package cat.company.wandervault.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.ZonedDateTime

@Dao
interface TripChatDao {
    @Query("SELECT * FROM trip_chat_sessions WHERE tripId = :tripId ORDER BY updatedAt DESC, id DESC")
    fun getSessionsByTripId(tripId: Int): Flow<List<TripChatSessionEntity>>

    @Query("SELECT * FROM trip_chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC, id ASC")
    fun getMessagesBySessionId(sessionId: Int): Flow<List<TripChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: TripChatSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertMessage(message: TripChatMessageEntity)

    @Query("UPDATE trip_chat_sessions SET updatedAt = :updatedAt WHERE id = :sessionId")
    suspend fun updateSessionUpdatedAt(sessionId: Int, updatedAt: ZonedDateTime)

    @Query("DELETE FROM trip_chat_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Int)
}
