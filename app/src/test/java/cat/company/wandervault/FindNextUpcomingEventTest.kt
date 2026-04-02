package cat.company.wandervault

import cat.company.wandervault.data.mlkit.TripDescriptionRepositoryImpl
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.repository.AppPreferencesRepository
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for [TripDescriptionRepositoryImpl.findNextUpcomingEvent].
 *
 * These tests verify that the function correctly identifies the single most immediate
 * upcoming itinerary event (destination arrival, destination departure, or transport-leg
 * departure/arrival) relative to a given [now] instant.
 */
class FindNextUpcomingEventTest {

    private val fakePreferences = object : AppPreferencesRepository {
        override fun getDefaultTimezone(): String? = null
        override fun setDefaultTimezone(zoneId: String?) = Unit
        override fun getAiLanguage(): String? = null
        override fun setAiLanguage(languageTag: String?) = Unit
    }

    private val repo = TripDescriptionRepositoryImpl(fakePreferences)

    private val now = ZonedDateTime.of(2024, 6, 10, 12, 0, 0, 0, ZoneOffset.UTC)

    private fun dest(
        id: Int = 1,
        name: String = "Test City",
        position: Int = 0,
        arrival: ZonedDateTime? = null,
        departure: ZonedDateTime? = null,
        transport: Transport? = null,
    ) = Destination(
        id = id,
        tripId = 1,
        name = name,
        position = position,
        arrivalDateTime = arrival,
        departureDateTime = departure,
        transport = transport,
    )

    // ── empty / no future events ──────────────────────────────────────────────

    @Test
    fun `returns null when destinations list is empty`() {
        val result = repo.findNextUpcomingEvent(emptyList(), now)
        assertNull(result)
    }

    @Test
    fun `returns null when all events are in the past`() {
        val destination = dest(
            arrival = now.minusHours(5),
            departure = now.minusHours(1),
        )
        val result = repo.findNextUpcomingEvent(listOf(destination), now)
        assertNull(result)
    }

    @Test
    fun `returns null when destinations have no dates at all`() {
        val destination = dest()
        val result = repo.findNextUpcomingEvent(listOf(destination), now)
        assertNull(result)
    }

    // ── destination departure ────────────────────────────────────────────────

    @Test
    fun `returns departure event when only future departure is present`() {
        val departure = now.plusHours(3)
        val destination = dest(name = "Paris", departure = departure)
        val result = repo.findNextUpcomingEvent(listOf(destination), now)
        assertNotNull(result)
        assertTrue("Expected departure mention", result!!.contains("Paris"))
        assertTrue("Expected departure keyword", result.contains("depart"))
    }

    // ── destination arrival ──────────────────────────────────────────────────

    @Test
    fun `returns arrival event when only future arrival is present`() {
        val arrival = now.plusHours(2)
        val destination = dest(name = "Rome", arrival = arrival)
        val result = repo.findNextUpcomingEvent(listOf(destination), now)
        assertNotNull(result)
        assertTrue("Expected arrival mention", result!!.contains("Rome"))
        assertTrue("Expected arrival keyword", result.contains("arrive"))
    }

    // ── earliest event wins ──────────────────────────────────────────────────

    @Test
    fun `returns the earliest future event among arrival and departure`() {
        val arrival = now.plusHours(1)
        val departure = now.plusHours(3)
        val destination = dest(name = "Berlin", arrival = arrival, departure = departure)
        val result = repo.findNextUpcomingEvent(listOf(destination), now)
        assertNotNull(result)
        // The arrival (1h from now) is earlier than the departure (3h).
        assertTrue("Expected arrive keyword for earliest event", result!!.contains("arrive"))
    }

    @Test
    fun `returns soonest event across multiple destinations`() {
        val destA = dest(id = 1, name = "Vienna", position = 0, departure = now.plusHours(5))
        val destB = dest(id = 2, name = "Prague", position = 1, arrival = now.plusHours(2))
        val result = repo.findNextUpcomingEvent(listOf(destA, destB), now)
        assertNotNull(result)
        // Prague arrival (2h) is sooner than Vienna departure (5h).
        assertTrue("Expected Prague in result", result!!.contains("Prague"))
    }

    // ── transport legs ───────────────────────────────────────────────────────

    @Test
    fun `returns transport leg departure when it is the earliest future event`() {
        val legDep = now.plusMinutes(90)
        val leg = TransportLeg(
            id = 1,
            transportId = 1,
            type = TransportType.FLIGHT,
            company = "AirTest",
            flightNumber = "AT100",
            departureDateTime = legDep,
        )
        val transport = Transport(id = 1, destinationId = 1, legs = listOf(leg))
        val destination = dest(
            name = "London",
            departure = now.plusHours(3),
            transport = transport,
        )
        val result = repo.findNextUpcomingEvent(listOf(destination), now)
        assertNotNull(result)
        // Leg departure (90 min) is earlier than destination departure (3h).
        assertTrue("Expected flight company", result!!.contains("AirTest"))
        assertTrue("Expected flight number", result.contains("AT100"))
        assertTrue("Expected departs keyword", result.contains("departs"))
    }

    @Test
    fun `returns transport leg arrival when it is the earliest future event`() {
        val legArr = now.plusMinutes(45)
        val leg = TransportLeg(
            id = 1,
            transportId = 1,
            type = TransportType.TRAIN,
            company = "RailCo",
            flightNumber = null,
            arrivalDateTime = legArr,
        )
        val transport = Transport(id = 1, destinationId = 1, legs = listOf(leg))
        val destination = dest(name = "Lyon", arrival = now.plusHours(2), transport = transport)
        val result = repo.findNextUpcomingEvent(listOf(destination), now)
        assertNotNull(result)
        // Leg arrival (45 min) is earlier than destination arrival (2h).
        assertTrue("Expected RailCo in result", result!!.contains("RailCo"))
        assertTrue("Expected arrives keyword", result.contains("arrives"))
    }

    // ── timezone correctness ─────────────────────────────────────────────────

    @Test
    fun `event description includes the timezone zone id`() {
        val departure = ZonedDateTime.of(2024, 6, 11, 9, 0, 0, 0, ZoneOffset.ofHours(5))
        val destination = dest(name = "Almaty", departure = departure)
        val result = repo.findNextUpcomingEvent(listOf(destination), now)
        assertNotNull(result)
        assertTrue("Expected timezone in description", result!!.contains("+05:00"))
    }
}
