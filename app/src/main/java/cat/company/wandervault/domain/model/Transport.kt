package cat.company.wandervault.domain.model

/**
 * Represents the transport for a single itinerary stop.
 *
 * A transport is the parent record that owns one or more ordered [TransportLeg] records
 * (the individual segments, e.g. taxi → flight → train) that carry the traveller from this
 * itinerary stop to the next one.
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param destinationId The [Destination.id] this transport departs **from**.
 * @param legs The ordered list of transport legs for this journey segment.
 */
data class Transport(
    val id: Int = 0,
    val destinationId: Int,
    val legs: List<TransportLeg> = emptyList(),
)
