package cat.company.wandervault.data.repository

import cat.company.wandervault.data.local.TripChatDao
import cat.company.wandervault.data.local.TripChatMessageEntity
import cat.company.wandervault.data.local.TripChatSessionEntity
import cat.company.wandervault.domain.model.TripChatMessage
import cat.company.wandervault.domain.model.TripChatMessageKind
import cat.company.wandervault.domain.model.TripChatSession
import cat.company.wandervault.domain.repository.TripChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import java.time.ZonedDateTime

class TripChatRepositoryImpl(
    private val dao: TripChatDao,
) : TripChatRepository {
    override fun getSessionsForTrip(tripId: Int): Flow<List<TripChatSession>> =
        dao.getSessionsByTripId(tripId).map { entities -> entities.map { it.toDomain() } }

    override fun getMessagesForSession(sessionId: Int): Flow<List<TripChatMessage>> =
        dao.getMessagesBySessionId(sessionId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun createSession(tripId: Int): Int {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        return dao.insertSession(
            TripChatSessionEntity(
                tripId = tripId,
                createdAt = now,
                updatedAt = now,
            ),
        ).toInt()
    }

    override suspend fun saveMessage(sessionId: Int, kind: TripChatMessageKind, text: String?) {
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        dao.insertMessage(
            TripChatMessageEntity(
                sessionId = sessionId,
                role = kind.name,
                text = text,
                createdAt = now,
            ),
        )
        dao.updateSessionUpdatedAt(sessionId = sessionId, updatedAt = now)
    }

    override suspend fun deleteSession(sessionId: Int) {
        dao.deleteSessionById(sessionId)
    }
}

private fun TripChatSessionEntity.toDomain() = TripChatSession(
    id = id,
    tripId = tripId,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

private fun TripChatMessageEntity.toDomain() = TripChatMessage(
    id = id,
    sessionId = sessionId,
    kind = TripChatMessageKind.entries.firstOrNull { it.name == role } ?: TripChatMessageKind.ERROR,
    text = text,
    createdAt = createdAt,
)
