package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

@Entity(
    tableName = "trip_chat_sessions",
    indices = [Index(value = ["tripId"])],
    foreignKeys = [
        ForeignKey(
            entity = TripEntity::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class TripChatSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tripId: Int,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
)
