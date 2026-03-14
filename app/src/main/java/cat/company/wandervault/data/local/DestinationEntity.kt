package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.ZonedDateTime

@Entity(
    tableName = "destinations",
    indices = [Index(value = ["tripId", "position"])],
)
data class DestinationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tripId: Int,
    val name: String,
    val position: Int,
    val arrivalDateTime: ZonedDateTime? = null,
    val departureDateTime: ZonedDateTime? = null,
    val notes: String? = null,
)
