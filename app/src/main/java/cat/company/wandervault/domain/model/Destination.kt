package cat.company.wandervault.domain.model

import java.time.ZonedDateTime

/**
 * Represents a single stop in a trip itinerary.
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param tripId The ID of the trip this destination belongs to.
 * @param name The name of the destination (e.g. "Paris", "Rome").
 * @param position Zero-based index used to order destinations within the trip.
 * @param arrivalDateTime When the traveller arrives at this destination (timezone-aware).
 *   `null` for the first (start) destination, which has no arrival.
 * @param departureDateTime When the traveller departs from this destination (timezone-aware).
 *   `null` for the last (end) destination, which has no departure.
 * @param transport The transport used to travel **from** this destination to the next one,
 *   containing one or more ordered [Transport.legs].  `null` for the last destination (no onward
 *   journey) or when none is set yet.
 * @param notes Free-text notes the traveller can jot down for this stop (e.g. things to see,
 *   tips, reminders).  `null` means no notes have been added yet.
 */
data class Destination(
    val id: Int = 0,
    val tripId: Int,
    val name: String,
    val position: Int,
    val arrivalDateTime: ZonedDateTime? = null,
    val departureDateTime: ZonedDateTime? = null,
    val transport: Transport? = null,
    val notes: String? = null,
)
