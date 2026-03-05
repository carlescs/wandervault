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
 */
data class Transport(
    val id: Int = 0,
    val destinationId: Int,
    val type: TransportType,
)
