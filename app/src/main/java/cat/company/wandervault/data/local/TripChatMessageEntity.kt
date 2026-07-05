package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

@Entity(
    tableName = "trip_chat_messages",
    indices = [Index(value = ["sessionId"])],
    foreignKeys = [
        ForeignKey(
            entity = TripChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TripChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val role: String,
    val text: String?,
    val createdAt: ZonedDateTime,
)
