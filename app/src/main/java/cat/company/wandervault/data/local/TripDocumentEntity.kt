package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "trip_documents",
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
data class TripDocumentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tripId: Int,
    val name: String,
    val localUri: String,
    val mimeType: String,
    val folder: String? = null,
    val extractedText: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
