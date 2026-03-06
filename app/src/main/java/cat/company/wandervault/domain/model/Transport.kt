package cat.company.wandervault.domain.model

/**
 * Represents a single transport leg within the journey from one itinerary stop to the next.
 *
 * A destination may have several ordered legs (e.g. taxi → flight → train). Each leg is stored
 * as a separate entity so that booking references and other metadata can be managed independently.
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param destinationId The [Destination.id] this transport departs **from**.
 * @param type The mode of transport used for this leg.
 * @param position Zero-based order of this leg among all legs for the same destination.
 * @param company The carrier or company name (e.g. airline, bus operator).
 * @param flightNumber The flight, train, or route number for this leg.
 * @param reservationConfirmationNumber The booking or reservation confirmation code.
 */
data class Transport(
    val id: Int = 0,
    val destinationId: Int,
    val type: TransportType,
    val position: Int = 0,
    val company: String? = null,
    val flightNumber: String? = null,
    val reservationConfirmationNumber: String? = null,
)
