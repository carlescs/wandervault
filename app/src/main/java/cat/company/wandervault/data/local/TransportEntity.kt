package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a single transport leg connecting one itinerary stop to the next.
 *
 * A destination may have multiple ordered legs (e.g. taxi → flight → train).  The unique
 * constraint on [destinationId] has been removed to allow this.  Legs within a destination are
 * ordered by [position] (zero-based).
 *
 * The foreign key to [DestinationEntity] uses `CASCADE` so that deleting a destination
 * automatically removes all of its transport legs.
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
    indices = [Index(value = ["destinationId"])],
)
data class TransportEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val destinationId: Int,
    /** Serialised [cat.company.wandervault.domain.model.TransportType] name. */
    val type: String,
    /** Zero-based order of this leg among all legs departing from the same destination. */
    val position: Int = 0,
    /** The carrier or company name (e.g. airline, bus operator). */
    val company: String? = null,
    /** The flight, train, or route number for this leg. */
    val flightNumber: String? = null,
    /** The booking or reservation confirmation code. */
    val reservationConfirmationNumber: String? = null,
)
