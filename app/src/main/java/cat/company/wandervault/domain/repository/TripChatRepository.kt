package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.TripChatMessage
import cat.company.wandervault.domain.model.TripChatMessageKind
import cat.company.wandervault.domain.model.TripChatSession
import kotlinx.coroutines.flow.Flow

/** Repository for persisted trip chat conversations. */
interface TripChatRepository {
    fun getSessionsForTrip(tripId: Int): Flow<List<TripChatSession>>
    fun getMessagesForSession(sessionId: Int): Flow<List<TripChatMessage>>
    suspend fun createSession(tripId: Int): Int
    suspend fun saveMessage(sessionId: Int, kind: TripChatMessageKind, text: String?)
    suspend fun deleteSession(sessionId: Int)
}
