package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a transport leg connecting one itinerary stop to the next.
 *
 * The foreign key to [DestinationEntity] uses `CASCADE` so that deleting a destination
 * automatically removes its transport record.
 */
@Entity(
    tableName = "transports",
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
data class TransportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val destinationId: Int,
    /** Serialised [cat.company.wandervault.domain.model.TransportType] name. */
    val type: String,
    /** The carrier or company name (e.g. airline, bus operator). */
    val company: String? = null,
    /** The flight, train, or route number for this leg. */
    val flightNumber: String? = null,
    /** The booking or reservation confirmation code. */
    val reservationConfirmationNumber: String? = null,
)
