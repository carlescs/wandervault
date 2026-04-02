package cat.company.wandervault.domain.model

import java.time.LocalDate
import java.time.ZonedDateTime

/**
 * Represents a trip.
 *
 * @param startDate The earliest date derived from the trip's itinerary, or `null` if no
 *   destinations with dates have been added yet.
 * @param endDate The latest date derived from the trip's itinerary, or `null` if no
 *   destinations with dates have been added yet.
 * @param defaultTimezone IANA timezone ID (e.g. `"Europe/Paris"`) used as the default when
 *   creating new destinations and legs in this trip.  `null` means the device's system default
 *   timezone is used.
 * @param nextStep The most recently generated "what's next" notice for the trip, or `null` if
 *   none has been generated yet.
 * @param nextStepDeadline The moment after which [nextStep] should be considered stale and
 *   recalculated.  Computed as the earliest upcoming destination event time when [nextStep] is
 *   saved.  `null` means the notice never auto-expires.
 * @param isArchived Whether this trip has been archived and hidden from the main trip list.
 */
data class Trip(
    val id: Int,
    val title: String,
    val imageUri: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val aiDescription: String? = null,
    val isFavorite: Boolean = false,
    val defaultTimezone: String? = null,
    val nextStep: String? = null,
    val nextStepDeadline: ZonedDateTime? = null,
    val isArchived: Boolean = false,
)
