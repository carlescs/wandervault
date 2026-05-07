package cat.company.wandervault.domain.model

import java.time.ZonedDateTime

/**
 * Returns the earliest future destination or activity time after [now], which becomes the expiry
 * deadline for cached "What's Next" text.
 *
 * Destination arrivals and departures both matter because either can invalidate the current
 * notice (for example, "arrive in Rome" becomes stale once the arrival passes, and "leave Rome
 * tomorrow" becomes stale once departure time passes).
 */
internal fun computeNextStepDeadline(
    destinations: List<Destination>,
    activities: List<Activity>,
    now: ZonedDateTime,
): ZonedDateTime? {
    val destinationTimes = destinations
        .flatMap { listOf(it.arrivalDateTime, it.departureDateTime) }
    val activityTimes = activities.map { it.dateTime }
    return (destinationTimes + activityTimes)
        .filterNotNull()
        .filter { it.isAfter(now) }
        .minOrNull()
}
