package cat.company.wandervault.domain.model

import java.time.ZonedDateTime

/**
 * Represents a single leg within the transport journey from one itinerary stop to the next.
 *
 * A [Transport] parent record groups one or more ordered [TransportLeg] records so that
 * multi-segment journeys (e.g. taxi → flight → train) can be modelled.  Each leg carries its
 * own booking references and transport mode.
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param transportId The [Transport.id] this leg belongs to.
 * @param type The mode of transport used for this leg.
 * @param position Zero-based order of this leg among all legs for the same transport.
 * @param stopName The name of the stop or place where this leg ends (e.g. an intermediate city
 *   or airport).  For the last leg this is typically the overall destination of the transport.
 *   `null` when not yet set.
 * @param company The carrier or company name (e.g. airline, bus operator).
 * @param flightNumber The flight, train, or route number for this leg.
 * @param reservationConfirmationNumber The booking or reservation confirmation code.
 * @param isDefault When `true` this leg's transport type icon is shown in the itinerary
 *   timeline.  Only one leg per transport should have this flag set; when no leg has it the
 *   first leg is used as a fallback.
 * @param departureDateTime When the traveller departs for this leg (timezone-aware).  For the
 *   first leg this matches the parent [Destination.departureDateTime].  `null` when not yet set.
 * @param arrivalDateTime When the traveller arrives at the end of this leg (timezone-aware).
 *   For the last leg this matches the next [Destination.arrivalDateTime].  `null` when not yet set.
 */
data class TransportLeg(
    val id: Int = 0,
    val transportId: Int,
    val type: TransportType,
    val position: Int = 0,
    val stopName: String? = null,
    val company: String? = null,
    val flightNumber: String? = null,
    val reservationConfirmationNumber: String? = null,
    val isDefault: Boolean = false,
    val departureDateTime: ZonedDateTime? = null,
    val arrivalDateTime: ZonedDateTime? = null,
)
