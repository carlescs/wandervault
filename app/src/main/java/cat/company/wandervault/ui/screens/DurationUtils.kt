package cat.company.wandervault.ui.screens

import java.time.Duration
import java.time.ZonedDateTime

/**
 * Formats this [Duration] as a compact, human-readable string such as "2h 35m", "1h", or "45m".
 * The result is always based on total hours + remaining minutes.
 */
internal fun Duration.formatted(): String {
    val h = toHours()
    val m = (toMinutes() % 60).toInt()
    return when {
        h > 0L && m > 0 -> "${h}h ${m}m"
        h > 0L -> "${h}h"
        else -> "${m}m"
    }
}

/**
 * Returns the positive [Duration] from this [ZonedDateTime] until [other], accounting for
 * different timezones by comparing UTC instants.  Returns `null` if either value is `null`,
 * or if [other] is not strictly after this instant.
 */
internal fun ZonedDateTime?.durationUntil(other: ZonedDateTime?): Duration? {
    if (this == null || other == null) return null
    val d = Duration.between(this, other)
    return if (!d.isNegative && !d.isZero) d else null
}
