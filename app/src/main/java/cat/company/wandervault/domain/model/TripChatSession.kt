package cat.company.wandervault.domain.model

import java.time.ZonedDateTime

/** A saved chat conversation for a trip. */
data class TripChatSession(
    val id: Int = 0,
    val tripId: Int,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
)
