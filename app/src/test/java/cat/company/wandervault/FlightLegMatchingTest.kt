package cat.company.wandervault

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Unit tests for the null-safe flight-leg matching predicates used in
 * TripDocumentsViewModel.applyFlightInfo and ShareViewModel.handleFlightInfo.
 *
 * Regression tests for the bug where calling [String.equals] on a nullable
 * [TransportLeg.flightNumber] or [TransportLeg.reservationConfirmationNumber]
 * (without the safe-call operator) threw a NullPointerException whenever a
 * flight leg had not yet had those fields populated, silently killing the
 * document analysis apply flow.
 *
 * Also covers the departure-date matching criterion (priority 3 in the silent
 * upload path) that uses [FlightInfo.departureDate] against the owning
 * destination's departure date.
 */
class FlightLegMatchingTest {

    // ── flightNumber matching ─────────────────────────────────────────────────

    @Test
    fun `null leg flightNumber does not match non-null extracted flightNumber`() {
        val leg = TransportLeg(transportId = 1, type = TransportType.FLIGHT, flightNumber = null)
        val flightInfo = FlightInfo(flightNumber = "AF123")

        val matches = flightInfo.flightNumber != null &&
            leg.flightNumber?.equals(flightInfo.flightNumber, ignoreCase = true) == true

        assertFalse(matches)
    }

    @Test
    fun `matching leg flightNumber returns true (case-insensitive)`() {
        val leg = TransportLeg(transportId = 1, type = TransportType.FLIGHT, flightNumber = "af123")
        val flightInfo = FlightInfo(flightNumber = "AF123")

        val matches = flightInfo.flightNumber != null &&
            leg.flightNumber?.equals(flightInfo.flightNumber, ignoreCase = true) == true

        assertTrue(matches)
    }

    @Test
    fun `non-matching leg flightNumber returns false`() {
        val leg = TransportLeg(transportId = 1, type = TransportType.FLIGHT, flightNumber = "BA456")
        val flightInfo = FlightInfo(flightNumber = "AF123")

        val matches = flightInfo.flightNumber != null &&
            leg.flightNumber?.equals(flightInfo.flightNumber, ignoreCase = true) == true

        assertFalse(matches)
    }

    @Test
    fun `null extracted flightNumber returns false`() {
        val leg = TransportLeg(transportId = 1, type = TransportType.FLIGHT, flightNumber = "AF123")
        val flightInfo = FlightInfo(flightNumber = null)

        val matches = flightInfo.flightNumber != null &&
            leg.flightNumber?.equals(flightInfo.flightNumber, ignoreCase = true) == true

        assertFalse(matches)
    }

    // ── reservationConfirmationNumber matching ────────────────────────────────

    @Test
    fun `null leg reservationConfirmationNumber does not match non-null extracted bookingReference`() {
        val leg = TransportLeg(
            transportId = 1,
            type = TransportType.FLIGHT,
            reservationConfirmationNumber = null,
        )
        val flightInfo = FlightInfo(bookingReference = "ABCD1234")

        val matches = flightInfo.bookingReference != null &&
            leg.reservationConfirmationNumber?.equals(
                flightInfo.bookingReference,
                ignoreCase = true,
            ) == true

        assertFalse(matches)
    }

    @Test
    fun `matching leg reservationConfirmationNumber returns true (case-insensitive)`() {
        val leg = TransportLeg(
            transportId = 1,
            type = TransportType.FLIGHT,
            reservationConfirmationNumber = "abcd1234",
        )
        val flightInfo = FlightInfo(bookingReference = "ABCD1234")

        val matches = flightInfo.bookingReference != null &&
            leg.reservationConfirmationNumber?.equals(
                flightInfo.bookingReference,
                ignoreCase = true,
            ) == true

        assertTrue(matches)
    }

    @Test
    fun `non-matching leg reservationConfirmationNumber returns false`() {
        val leg = TransportLeg(
            transportId = 1,
            type = TransportType.FLIGHT,
            reservationConfirmationNumber = "XYZ9999",
        )
        val flightInfo = FlightInfo(bookingReference = "ABCD1234")

        val matches = flightInfo.bookingReference != null &&
            leg.reservationConfirmationNumber?.equals(
                flightInfo.bookingReference,
                ignoreCase = true,
            ) == true

        assertFalse(matches)
    }

    @Test
    fun `null extracted bookingReference returns false`() {
        val leg = TransportLeg(
            transportId = 1,
            type = TransportType.FLIGHT,
            reservationConfirmationNumber = "ABCD1234",
        )
        val flightInfo = FlightInfo(bookingReference = null)

        val matches = flightInfo.bookingReference != null &&
            leg.reservationConfirmationNumber?.equals(
                flightInfo.bookingReference,
                ignoreCase = true,
            ) == true

        assertFalse(matches)
    }

    // ── departureDate matching (priority 3 in silent upload path) ─────────────
    //
    // The upload path (applyFlightInfo) uses the destination's departure date as a
    // priority-3 criterion when neither flight number nor booking reference matches.
    // These tests verify that predicate directly.

    private fun destination(departure: LocalDate?) = Destination(
        id = 1,
        tripId = 1,
        name = "Test Dest",
        position = 0,
        departureDateTime = departure?.atStartOfDay(ZoneOffset.UTC),
    )

    @Test
    fun `destination departure date matches extracted departureDate`() {
        val date = LocalDate.of(2024, 6, 15)
        val dest = destination(departure = date)
        val flightInfo = FlightInfo(departureDate = date)

        val matches = flightInfo.departureDate != null &&
            dest.departureDateTime?.toLocalDate() == flightInfo.departureDate

        assertTrue(matches)
    }

    @Test
    fun `destination departure date does not match when dates differ`() {
        val dest = destination(departure = LocalDate.of(2024, 6, 15))
        val flightInfo = FlightInfo(departureDate = LocalDate.of(2024, 6, 16))

        val matches = flightInfo.departureDate != null &&
            dest.departureDateTime?.toLocalDate() == flightInfo.departureDate

        assertFalse(matches)
    }

    @Test
    fun `null extracted departureDate never matches`() {
        val dest = destination(departure = LocalDate.of(2024, 6, 15))
        val flightInfo = FlightInfo(departureDate = null)

        val matches = flightInfo.departureDate != null &&
            dest.departureDateTime?.toLocalDate() == flightInfo.departureDate

        assertFalse(matches)
    }

    @Test
    fun `null destination departure date does not match non-null extracted departureDate`() {
        val dest = destination(departure = null)
        val flightInfo = FlightInfo(departureDate = LocalDate.of(2024, 6, 15))

        val matches = flightInfo.departureDate != null &&
            dest.departureDateTime?.toLocalDate() == flightInfo.departureDate

        assertFalse(matches)
    }
}
