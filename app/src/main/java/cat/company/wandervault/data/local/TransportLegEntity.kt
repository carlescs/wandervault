package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Room entity for a single transport leg that belongs to a parent [TransportEntity].
 *
 * A [TransportEntity] (representing the overall journey departing from one itinerary stop) may
 * have multiple ordered legs (e.g. taxi → flight → train).  Legs within a transport are ordered
 * by [position] (zero-based).
 *
 * The foreign key to [TransportEntity] uses `CASCADE` so that deleting a transport parent
 * automatically removes all of its legs.
 */
@Entity(
    tableName = "transport_legs",
    foreignKeys = [
        ForeignKey(
            entity = TransportEntity::class,
            parentColumns = ["id"],
            childColumns = ["transportId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["transportId"])],
)
data class TransportLegEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val transportId: Int,
    /** Serialised [cat.company.wandervault.domain.model.TransportType] name. */
    val type: String,
    /** Zero-based order of this leg among all legs for the same transport. */
    val position: Int = 0,
    /** The name of the stop or place where this leg ends (intermediate city, airport, etc.). */
    val stopName: String? = null,
    /** The carrier or company name (e.g. airline, bus operator). */
    val company: String? = null,
    /** The flight, train, or route number for this leg. */
    val flightNumber: String? = null,
    /** The booking or reservation confirmation code. */
    val reservationConfirmationNumber: String? = null,
    /**
     * When `true` this leg's transport type icon is displayed in the itinerary timeline.
     * Only one leg per transport should have this flag set.
     */
    val isDefault: Boolean = false,
    /** When the traveller departs for this leg.  Stored as ISO-8601 string via [DateConverters]. */
    val departureDateTime: LocalDateTime? = null,
    /** When the traveller arrives at the end of this leg.  Stored as ISO-8601 string via [DateConverters]. */
    val arrivalDateTime: LocalDateTime? = null,
)
