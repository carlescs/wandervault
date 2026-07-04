package cat.company.wandervault.domain.model

import java.time.ZonedDateTime

/** A single persisted message inside a trip chat session. */
data class TripChatMessage(
    val id: Int = 0,
    val sessionId: Int,
    val kind: TripChatMessageKind,
    val text: String?,
    val createdAt: ZonedDateTime,
)
