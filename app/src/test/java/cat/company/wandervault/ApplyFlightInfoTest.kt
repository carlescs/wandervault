package cat.company.wandervault

import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.ui.screens.applyFlightInfo
import cat.company.wandervault.ui.screens.toZonedDeparture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Unit tests for [TransportLeg.applyFlightInfo] and [FlightInfo.toZonedDeparture].
 *
 * These tests verify:
 * - Text fill-blank rule (existing non-blank values preserved).
 * - Departure time applied to an existing departureDateTime (time replaced, date/zone kept).
 * - Departure datetime constructed from date+time when leg has none.
 * - Arrival time applied to an existing arrivalDateTime (time replaced, date/zone kept).
 * - No new arrivalDateTime created when leg has none.
 * - No change returned when FlightInfo adds nothing new.
 * - Zone preservation across DST-relevant zones.
 */
class ApplyFlightInfoTest {

    private val utc = ZoneId.of("UTC")
    private val london = ZoneId.of("Europe/London")
    private val berlin = ZoneId.of("Europe/Berlin")

    // ── toZonedDeparture ─────────────────────────────────────────────────────

    @Test
    fun `toZonedDeparture returns null when departureDate is null`() {
        val fi = FlightInfo(departureTime = LocalTime.of(10, 0))
        assertNull(fi.toZonedDeparture())
    }

    @Test
    fun `toZonedDeparture returns null when departureTime is null`() {
        val fi = FlightInfo(departureDate = LocalDate.of(2025, 6, 1))
        assertNull(fi.toZonedDeparture())
    }

    @Test
    fun `toZonedDeparture constructs correct ZonedDateTime from date and time`() {
        val fi = FlightInfo(
            departureDate = LocalDate.of(2025, 7, 10),
            departureTime = LocalTime.of(8, 30),
        )
        val result = fi.toZonedDeparture(utc)!!
        assertEquals(2025, result.year)
        assertEquals(7, result.monthValue)
        assertEquals(10, result.dayOfMonth)
        assertEquals(8, result.hour)
        assertEquals(30, result.minute)
        assertEquals(utc, result.zone)
    }

    // ── Text fill-blank rule ──────────────────────────────────────────────────

    @Test
    fun `existing non-blank company is preserved`() {
        val leg = leg(company = "Existing Airline")
        val result = leg.applyFlightInfo(FlightInfo(airline = "New Airline"))
        assertEquals("Existing Airline", result.company)
    }

    @Test
    fun `blank company is replaced with extracted airline`() {
        val leg = leg(company = "")
        val result = leg.applyFlightInfo(FlightInfo(airline = "Lufthansa"))
        assertEquals("Lufthansa", result.company)
    }

    @Test
    fun `null company is replaced with extracted airline`() {
        val leg = leg(company = null)
        val result = leg.applyFlightInfo(FlightInfo(airline = "Iberia"))
        assertEquals("Iberia", result.company)
    }

    @Test
    fun `existing non-blank flightNumber is preserved`() {
        val leg = leg(flightNumber = "AB123")
        val result = leg.applyFlightInfo(FlightInfo(flightNumber = "CD456"))
        assertEquals("AB123", result.flightNumber)
    }

    @Test
    fun `existing non-blank stopName is preserved`() {
        val leg = leg(stopName = "Existing Stop")
        val result = leg.applyFlightInfo(FlightInfo(arrivalPlace = "New Stop"))
        assertEquals("Existing Stop", result.stopName)
    }

    // ── departureDateTime time update ─────────────────────────────────────────

    @Test
    fun `departure time updates time portion of existing departureDateTime, preserving date and zone`() {
        val existingDt = ZonedDateTime.of(2025, 6, 15, 7, 0, 0, 0, berlin)
        val leg = leg(departureDateTime = existingDt)
        val result = leg.applyFlightInfo(FlightInfo(departureTime = LocalTime.of(9, 30)))
        assertEquals(9, result.departureDateTime!!.hour)
        assertEquals(30, result.departureDateTime.minute)
        assertEquals(2025, result.departureDateTime.year)
        assertEquals(6, result.departureDateTime.monthValue)
        assertEquals(15, result.departureDateTime.dayOfMonth)
        assertEquals(berlin, result.departureDateTime.zone)
    }

    @Test
    fun `departure time from flightInfo constructs new departureDateTime when leg has none`() {
        val leg = leg(departureDateTime = null)
        val fi = FlightInfo(
            departureDate = LocalDate.of(2025, 8, 20),
            departureTime = LocalTime.of(14, 45),
        )
        val result = leg.applyFlightInfo(fi)
        assertEquals(14, result.departureDateTime!!.hour)
        assertEquals(45, result.departureDateTime.minute)
        assertEquals(2025, result.departureDateTime.year)
        assertEquals(8, result.departureDateTime.monthValue)
        assertEquals(20, result.departureDateTime.dayOfMonth)
    }

    @Test
    fun `departure time alone (no date) does not create departureDateTime when leg has none`() {
        val leg = leg(departureDateTime = null)
        val result = leg.applyFlightInfo(FlightInfo(departureTime = LocalTime.of(8, 0)))
        // No departureDate, so toZonedDeparture() returns null and departureDateTime stays null.
        assertNull(result.departureDateTime)
    }

    @Test
    fun `no departureTime in flightInfo leaves existing departureDateTime unchanged`() {
        val existingDt = ZonedDateTime.of(2025, 1, 1, 10, 0, 0, 0, utc)
        val leg = leg(departureDateTime = existingDt)
        val result = leg.applyFlightInfo(FlightInfo())
        assertEquals(existingDt, result.departureDateTime)
    }

    // ── arrivalDateTime time update ───────────────────────────────────────────

    @Test
    fun `arrival time updates time portion of existing arrivalDateTime, preserving date and zone`() {
        val existingDt = ZonedDateTime.of(2025, 6, 15, 11, 0, 0, 0, london)
        val leg = leg(arrivalDateTime = existingDt)
        val result = leg.applyFlightInfo(FlightInfo(arrivalTime = LocalTime.of(12, 30)))
        assertEquals(12, result.arrivalDateTime!!.hour)
        assertEquals(30, result.arrivalDateTime.minute)
        assertEquals(2025, result.arrivalDateTime.year)
        assertEquals(6, result.arrivalDateTime.monthValue)
        assertEquals(15, result.arrivalDateTime.dayOfMonth)
        assertEquals(london, result.arrivalDateTime.zone)
    }

    @Test
    fun `arrival time does not create arrivalDateTime when leg has none`() {
        val leg = leg(arrivalDateTime = null)
        val result = leg.applyFlightInfo(FlightInfo(arrivalTime = LocalTime.of(15, 0)))
        assertNull(result.arrivalDateTime)
    }

    @Test
    fun `no arrivalTime in flightInfo leaves existing arrivalDateTime unchanged`() {
        val existingDt = ZonedDateTime.of(2025, 3, 10, 14, 0, 0, 0, utc)
        val leg = leg(arrivalDateTime = existingDt)
        val result = leg.applyFlightInfo(FlightInfo())
        assertEquals(existingDt, result.arrivalDateTime)
    }

    // ── No-op when nothing changes ────────────────────────────────────────────

    @Test
    fun `applyFlightInfo returns equal leg when FlightInfo contributes no new data`() {
        val leg = leg(
            company = "Lufthansa",
            flightNumber = "LH1234",
            reservationConfirmationNumber = "ABC123",
            stopName = "London",
            departureDateTime = ZonedDateTime.of(2025, 6, 1, 10, 0, 0, 0, utc),
            arrivalDateTime = ZonedDateTime.of(2025, 6, 1, 12, 0, 0, 0, utc),
        )
        val result = leg.applyFlightInfo(FlightInfo())
        assertEquals(leg, result)
    }

    // ── DST zone preservation ─────────────────────────────────────────────────

    @Test
    fun `zone is preserved when updating departure time in a DST-aware zone`() {
        val summerDt = ZonedDateTime.of(2025, 7, 1, 8, 0, 0, 0, london) // BST (UTC+1)
        val leg = leg(departureDateTime = summerDt)
        val result = leg.applyFlightInfo(FlightInfo(departureTime = LocalTime.of(9, 30)))
        assertEquals(london, result.departureDateTime!!.zone)
        assertEquals(9, result.departureDateTime.hour)
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private fun leg(
        company: String? = null,
        flightNumber: String? = null,
        reservationConfirmationNumber: String? = null,
        stopName: String? = null,
        departureDateTime: ZonedDateTime? = null,
        arrivalDateTime: ZonedDateTime? = null,
    ) = TransportLeg(
        transportId = 1,
        type = TransportType.FLIGHT,
        company = company,
        flightNumber = flightNumber,
        reservationConfirmationNumber = reservationConfirmationNumber,
        stopName = stopName,
        departureDateTime = departureDateTime,
        arrivalDateTime = arrivalDateTime,
    )
}
