package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing the transport for a single itinerary stop.
 *
 * A transport is the parent record that groups one or more ordered [TransportLegEntity] records
 * (the individual segments, e.g. taxi → flight → train) that carry the traveller from this
 * itinerary stop to the next one.
 *
 * The foreign key to [DestinationEntity] uses `CASCADE` so that deleting a destination
 * automatically removes its transport and all associated legs.
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
)
