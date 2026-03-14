package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val imageUri: String? = null,
    val aiDescription: String? = null,
    val isFavorite: Boolean = false,
    /** IANA timezone ID used as the default for new destinations and legs in this trip. */
    val defaultTimezone: String? = null,
)
