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
 * @param transport The transport leg used to travel **from** this destination to the next one.
 *   `null` for the last destination (no onward journey) or when not yet set.
 *   Stored as a separate [Transport] entity so that future properties (booking references,
 *   attached documents, notes) can be added without touching this class.
 * @param latitude Optional GPS latitude of the destination (decimal degrees, WGS-84).
 *   When provided together with [longitude], the destination is shown as a pin on the map view.
 * @param longitude Optional GPS longitude of the destination (decimal degrees, WGS-84).
 */
data class Destination(
    val id: Int = 0,
    val tripId: Int,
    val name: String,
    val position: Int,
    val arrivalDateTime: LocalDateTime? = null,
    val departureDateTime: LocalDateTime? = null,
    val transport: Transport? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)
