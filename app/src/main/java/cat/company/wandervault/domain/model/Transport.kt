package cat.company.wandervault.domain.model

/**
 * Represents the transport leg that connects one itinerary stop to the next.
 *
 * Stored as a separate entity so that future properties (e.g. booking references,
 * attached documents, notes) can be added without touching [Destination].
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param destinationId The [Destination.id] this transport departs **from**.
 * @param type The mode of transport used for this leg.
 * @param company The carrier or company name (e.g. airline, bus operator).
 * @param flightNumber The flight, train, or route number for this leg.
 * @param reservationConfirmationNumber The booking or reservation confirmation code.
 */
data class Transport(
    val id: Int = 0,
    val destinationId: Int,
    val type: TransportType,
    val company: String? = null,
    val flightNumber: String? = null,
    val reservationConfirmationNumber: String? = null,
)
