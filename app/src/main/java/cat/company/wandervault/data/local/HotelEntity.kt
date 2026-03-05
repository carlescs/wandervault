package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Room entity for a hotel reservation associated with a destination stop.
 *
 * The foreign key to [DestinationEntity] uses `CASCADE` so that deleting a destination
 * automatically removes its hotel record.
 */
@Entity(
    tableName = "hotels",
    foreignKeys = [
        ForeignKey(
            entity = DestinationEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["destinationId"], unique = true)],
)
data class HotelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val destinationId: Int,
    val name: String,
    val address: String,
    val checkInDate: LocalDate? = null,
    val checkOutDate: LocalDate? = null,
    val confirmationNumber: String,
    val notes: String,
)
