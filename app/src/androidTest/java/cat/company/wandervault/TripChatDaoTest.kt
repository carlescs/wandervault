package cat.company.wandervault

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import cat.company.wandervault.data.local.TripChatDao
import cat.company.wandervault.data.local.TripChatMessageEntity
import cat.company.wandervault.data.local.TripChatSessionEntity
import cat.company.wandervault.data.local.TripEntity
import cat.company.wandervault.data.local.WanderVaultDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Instrumented tests for [TripChatDao].
 *
 * Uses an in-memory Room database so no persistent storage is affected.
 */
@RunWith(AndroidJUnit4::class)
class TripChatDaoTest {

    private lateinit var db: WanderVaultDatabase
    private lateinit var dao: TripChatDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WanderVaultDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.tripChatDao()

        // Insert a parent trip so FK constraints are satisfied.
        runTest {
            db.tripDao().insert(TripEntity(id = 1, title = "Test Trip"))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ── Session retrieval ─────────────────────────────────────────────────────

    @Test
    fun insertAndGetSessions() = runTest {
        val t1 = ZonedDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        val t2 = ZonedDateTime.of(2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC)
        dao.insertSession(TripChatSessionEntity(id = 1, tripId = 1, createdAt = t1, updatedAt = t1))
        dao.insertSession(TripChatSessionEntity(id = 2, tripId = 1, createdAt = t2, updatedAt = t2))

        val sessions = dao.getSessionsByTripId(tripId = 1).first()

        // Sessions ordered by updatedAt DESC.
        assertEquals(2, sessions.size)
        assertEquals(2, sessions[0].id)
        assertEquals(1, sessions[1].id)
    }

    @Test
    fun getSessionsByTripId_onlyReturnsSameTrip() = runTest {
        runTest { db.tripDao().insert(TripEntity(id = 2, title = "Other Trip")) }
        val t = ZonedDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        dao.insertSession(TripChatSessionEntity(id = 1, tripId = 1, createdAt = t, updatedAt = t))
        dao.insertSession(TripChatSessionEntity(id = 2, tripId = 2, createdAt = t, updatedAt = t))

        val sessions = dao.getSessionsByTripId(tripId = 1).first()

        assertEquals(1, sessions.size)
        assertEquals(1, sessions[0].id)
    }

    // ── Message retrieval ─────────────────────────────────────────────────────

    @Test
    fun insertAndGetMessages_orderedByCreatedAtAsc() = runTest {
        val sessionTime = ZonedDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        dao.insertSession(TripChatSessionEntity(id = 1, tripId = 1, createdAt = sessionTime, updatedAt = sessionTime))

        val t1 = ZonedDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        val t2 = ZonedDateTime.of(2026, 1, 1, 10, 1, 0, 0, ZoneOffset.UTC)
        dao.insertMessage(TripChatMessageEntity(id = 10, sessionId = 1, role = "USER", text = "Hello", createdAt = t1))
        dao.insertMessage(TripChatMessageEntity(id = 11, sessionId = 1, role = "AI", text = "Hi", createdAt = t2))

        val messages = dao.getMessagesBySessionId(sessionId = 1).first()

        assertEquals(2, messages.size)
        assertEquals(10, messages[0].id)
        assertEquals(11, messages[1].id)
    }

    // ── updateSessionUpdatedAt ────────────────────────────────────────────────

    @Test
    fun updateSessionUpdatedAt_changesValueAndReordersResults() = runTest {
        val t1 = ZonedDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        val t2 = ZonedDateTime.of(2026, 1, 2, 10, 0, 0, 0, ZoneOffset.UTC)
        val t3 = ZonedDateTime.of(2026, 1, 3, 10, 0, 0, 0, ZoneOffset.UTC)
        dao.insertSession(TripChatSessionEntity(id = 1, tripId = 1, createdAt = t1, updatedAt = t1))
        dao.insertSession(TripChatSessionEntity(id = 2, tripId = 1, createdAt = t2, updatedAt = t2))

        // Bump session 1 to be the most recently updated.
        dao.updateSessionUpdatedAt(sessionId = 1, updatedAt = t3)

        val sessions = dao.getSessionsByTripId(tripId = 1).first()
        assertEquals(2, sessions.size)
        assertEquals(1, sessions[0].id)
        assertEquals(t3, sessions[0].updatedAt)
    }

    // ── Cascade delete session → messages ─────────────────────────────────────

    @Test
    fun deleteSession_cascadesToMessages() = runTest {
        val t = ZonedDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        dao.insertSession(TripChatSessionEntity(id = 1, tripId = 1, createdAt = t, updatedAt = t))
        dao.insertMessage(TripChatMessageEntity(sessionId = 1, role = "USER", text = "Hi", createdAt = t))

        dao.deleteSessionById(sessionId = 1)

        val sessions = dao.getSessionsByTripId(tripId = 1).first()
        val messages = dao.getMessagesBySessionId(sessionId = 1).first()
        assertTrue(sessions.isEmpty())
        assertTrue(messages.isEmpty())
    }

    // ── Cascade delete trip → sessions → messages ─────────────────────────────

    @Test
    fun deleteTrip_cascadesToSessionsAndMessages() = runTest {
        runTest { db.tripDao().insert(TripEntity(id = 3, title = "Trip To Delete")) }
        val t = ZonedDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC)
        dao.insertSession(TripChatSessionEntity(id = 5, tripId = 3, createdAt = t, updatedAt = t))
        dao.insertMessage(TripChatMessageEntity(sessionId = 5, role = "USER", text = "Hello", createdAt = t))

        db.tripDao().delete(TripEntity(id = 3, title = "Trip To Delete"))

        val sessions = dao.getSessionsByTripId(tripId = 3).first()
        val messages = dao.getMessagesBySessionId(sessionId = 5).first()
        assertTrue(sessions.isEmpty())
        assertTrue(messages.isEmpty())
    }
}
