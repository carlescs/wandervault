package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "destinations")
data class DestinationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tripId: Int,
    val name: String,
    val position: Int,
    val arrivalDateTime: LocalDateTime? = null,
    val departureDateTime: LocalDateTime? = null,
)
