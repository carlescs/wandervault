package cat.company.wandervault.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "hotels",
    indices = [Index(value = ["destinationId"], unique = true)],
    foreignKeys = [
        ForeignKey(
            entity = DestinationEntity::class,
            parentColumns = ["id"],
            childColumns = ["destinationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class HotelEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val destinationId: Int,
    val name: String = "",
    val address: String = "",
    val reservationNumber: String = "",
)
