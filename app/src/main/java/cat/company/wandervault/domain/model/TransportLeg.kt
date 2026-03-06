package cat.company.wandervault.domain.model

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
 * @param company The carrier or company name (e.g. airline, bus operator).
 * @param flightNumber The flight, train, or route number for this leg.
 * @param reservationConfirmationNumber The booking or reservation confirmation code.
 */
data class TransportLeg(
    val id: Int = 0,
    val transportId: Int,
    val type: TransportType,
    val position: Int = 0,
    val company: String? = null,
    val flightNumber: String? = null,
    val reservationConfirmationNumber: String? = null,
)
