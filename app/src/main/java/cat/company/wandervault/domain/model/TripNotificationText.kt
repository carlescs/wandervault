package cat.company.wandervault.domain.model

import java.time.ZonedDateTime

/**
 * Returns the stored "What's Next" text that is still valid for notifications at [now], or
 * `null` when there is no text or its expiry deadline has passed.
 */
internal fun Trip.activeNotificationNextStep(now: ZonedDateTime): String? {
    val trimmedNextStep = nextStep?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (nextStepDeadline == null || nextStepDeadline.isAfter(now)) trimmedNextStep else null
}

/** Returns `true` when a stored notification notice exists but its expiry deadline has passed. */
internal fun Trip.hasExpiredNotificationNextStep(now: ZonedDateTime): Boolean =
    !nextStep.isNullOrBlank() && nextStepDeadline?.isAfter(now) == false
