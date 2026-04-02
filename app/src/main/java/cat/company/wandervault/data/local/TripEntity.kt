package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val imageUri: String? = null,
    val aiDescription: String? = null,
    val isFavorite: Boolean = false,
    /** IANA timezone ID used as the default for new destinations and legs in this trip. */
    val defaultTimezone: String? = null,
    /** The most recently generated "what's next" notice, or `null` if none exists. */
    val nextStep: String? = null,
    /**
     * The moment after which [nextStep] should be recalculated.  Set to the earliest upcoming
     * destination event time when [nextStep] is saved.  `null` means the notice never auto-expires.
     */
    val nextStepDeadline: ZonedDateTime? = null,
    /** Whether this trip has been archived and hidden from the main trip list. */
    val isArchived: Boolean = false,
)
