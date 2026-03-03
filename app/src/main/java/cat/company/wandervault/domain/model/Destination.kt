package cat.company.wandervault.domain.model

import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Represents a single stop in a trip itinerary.
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param tripId The ID of the trip this destination belongs to.
 * @param name The name of the destination (e.g. "Paris", "Rome").
 * @param position Zero-based index used to order destinations within the trip.
 * @param arrivalDateTime When the traveller arrives at this destination.
 *   `null` for the first (start) destination, which has no arrival.
 * @param departureDateTime When the traveller departs from this destination.
 *   `null` for the last (end) destination, which has no departure.
 * @param timezone The local timezone of this destination (e.g. `ZoneId.of("Europe/Paris")`).
 *   `null` means no timezone has been specified; callers should fall back to the device timezone.
 */
data class Destination(
    val id: Int = 0,
    val tripId: Int,
    val name: String,
    val position: Int,
    val arrivalDateTime: LocalDateTime? = null,
    val departureDateTime: LocalDateTime? = null,
    val timezone: ZoneId? = null,
)
