package cat.company.wandervault.domain.model

import java.time.LocalDateTime

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
 * @param transports The ordered list of transport legs used to travel **from** this destination to
 *   the next one.  Empty for the last destination (no onward journey) or when none are set yet.
 *   Each [Transport] is stored as a separate entity so that booking references and other metadata
 *   can be managed per-leg.
 */
data class Destination(
    val id: Int = 0,
    val tripId: Int,
    val name: String,
    val position: Int,
    val arrivalDateTime: LocalDateTime? = null,
    val departureDateTime: LocalDateTime? = null,
    val transports: List<Transport> = emptyList(),
)
