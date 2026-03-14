package cat.company.wandervault.data.local

import androidx.room.ColumnInfo
import java.time.ZonedDateTime

/**
 * Lightweight Room projection used solely to compute trip date ranges.
 * Contains only the columns required for min/max date calculation.
 */
data class DestinationDateProjection(
    @ColumnInfo(name = "tripId") val tripId: Int,
    @ColumnInfo(name = "arrivalDateTime") val arrivalDateTime: ZonedDateTime?,
    @ColumnInfo(name = "departureDateTime") val departureDateTime: ZonedDateTime?,
)
