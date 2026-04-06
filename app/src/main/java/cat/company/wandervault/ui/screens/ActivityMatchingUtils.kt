package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.ActivityInfo
import cat.company.wandervault.domain.model.Destination
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Returns `true` when [activityInfo] has a date that falls within the destination's stay period
 * (between arrival and departure). Missing bounds on either side are treated as open (no constraint).
 */
internal fun Destination.containsActivityDate(activityInfo: ActivityInfo): Boolean {
    val actDate = activityInfo.date ?: return true
    val destArrival = arrivalDateTime?.toLocalDate()
    val destDeparture = departureDateTime?.toLocalDate()
    if (destArrival != null && actDate.isBefore(destArrival)) return false
    if (destDeparture != null && actDate.isAfter(destDeparture)) return false
    return true
}

/**
 * Constructs a [ZonedDateTime] from [ActivityInfo.date] and [ActivityInfo.time]
 * using the system default zone. Returns `null` when the date is absent.
 * When only the date is available (no time), midnight is used.
 */
internal fun ActivityInfo.toZonedDateTime(zone: ZoneId = ZoneId.systemDefault()): ZonedDateTime? {
    val d = date ?: return null
    return if (time != null) {
        ZonedDateTime.of(d, time, zone)
    } else {
        d.atStartOfDay(zone)
    }
}
