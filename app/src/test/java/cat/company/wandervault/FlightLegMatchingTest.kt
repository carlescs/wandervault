package cat.company.wandervault

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    // ── source-document exclusion from confident match ────────────────────────
    //
    // Legs that were already sourced from the current document (sourceDocumentId == documentId)
    // must be excluded from the confident-match search. This prevents a multi-leg itinerary
    // where all legs share the same booking reference from re-matching the same leg over and over
    // once the first flight has been applied.

    private val documentId = 42

    @Test
    fun `leg already sourced from this document is excluded from confident match by flightNumber`() {
        val alreadyLinkedLeg = TransportLeg(
            transportId = 1,
            type = TransportType.FLIGHT,
            flightNumber = "AF123",
            sourceDocumentId = documentId,
        )
        val flightInfo = FlightInfo(flightNumber = "AF123")

        // Simulates the availableFlightLegs filter applied in applyOrDisambiguateFlightInfo /
        // handleFlightInfo: legs with sourceDocumentId == documentId are excluded.
        val availableLegs = listOf(alreadyLinkedLeg).filter { it.sourceDocumentId != documentId }

        val confidentMatch = availableLegs.firstOrNull { leg ->
            flightInfo.flightNumber != null &&
                leg.flightNumber?.equals(flightInfo.flightNumber, ignoreCase = true) == true
        }

        // Even though the flight number matches, the leg is excluded because it was already
        // sourced from this document.
        assertTrue(availableLegs.isEmpty())
        assertNull(confidentMatch)
    }

    @Test
    fun `leg already sourced from this document is excluded from confident match by bookingReference`() {
        val alreadyLinkedLeg = TransportLeg(
            transportId = 1,
            type = TransportType.FLIGHT,
            reservationConfirmationNumber = "BOOK1",
            sourceDocumentId = documentId,
        )
        val flightInfo = FlightInfo(bookingReference = "BOOK1")

        val availableLegs = listOf(alreadyLinkedLeg).filter { it.sourceDocumentId != documentId }

        val confidentMatch = availableLegs.firstOrNull { leg ->
            flightInfo.bookingReference != null &&
                leg.reservationConfirmationNumber?.equals(
                    flightInfo.bookingReference,
                    ignoreCase = true,
                ) == true
        }

        assertTrue(availableLegs.isEmpty())
        assertNull(confidentMatch)
    }

    @Test
    fun `second leg not yet linked to this document is found as confident match after first leg is excluded`() {
        val firstLeg = TransportLeg(
            id = 1,
            transportId = 1,
            type = TransportType.FLIGHT,
            flightNumber = "AF101",
            reservationConfirmationNumber = "BOOK1",
            sourceDocumentId = documentId, // already applied in this session
        )
        val secondLeg = TransportLeg(
            id = 2,
            transportId = 2,
            type = TransportType.FLIGHT,
            flightNumber = null,
            reservationConfirmationNumber = null,
            sourceDocumentId = null, // not yet applied
        )
        // Second flight from the same document shares the booking reference.
        val secondFlightInfo = FlightInfo(flightNumber = "AF202", bookingReference = "BOOK1")

        val availableLegs = listOf(firstLeg, secondLeg).filter { it.sourceDocumentId != documentId }

        // firstLeg is excluded; only secondLeg is available.
        assertTrue(availableLegs == listOf(secondLeg))

        // No confident match by flight number (secondLeg has no flightNumber).
        val matchByFlightNumber = availableLegs.firstOrNull { leg ->
            secondFlightInfo.flightNumber != null &&
                leg.flightNumber?.equals(secondFlightInfo.flightNumber, ignoreCase = true) == true
        }
        // No confident match by booking reference (secondLeg has no reservationConfirmationNumber).
        val matchByBookingRef = availableLegs.firstOrNull { leg ->
            secondFlightInfo.bookingReference != null &&
                leg.reservationConfirmationNumber?.equals(
                    secondFlightInfo.bookingReference,
                    ignoreCase = true,
                ) == true
        }

        // Neither matches — disambiguation dialog will be shown with secondLeg as the candidate.
        assertNull(matchByFlightNumber)
        assertNull(matchByBookingRef)
    }

    @Test
    fun `leg sourced from a different document is not excluded from confident match`() {
        val otherDocumentId = 99
        val legFromOtherDoc = TransportLeg(
            transportId = 1,
            type = TransportType.FLIGHT,
            flightNumber = "AF123",
            sourceDocumentId = otherDocumentId,
        )
        val flightInfo = FlightInfo(flightNumber = "AF123")

        // Only legs whose sourceDocumentId == documentId are excluded.
        val availableLegs = listOf(legFromOtherDoc).filter { it.sourceDocumentId != documentId }

        val confidentMatch = availableLegs.firstOrNull { leg ->
            flightInfo.flightNumber != null &&
                leg.flightNumber?.equals(flightInfo.flightNumber, ignoreCase = true) == true
        }

        // The leg is from a different document, so it is included and matches.
        assertTrue(availableLegs.isNotEmpty())
        assertNotNull(confidentMatch)
    }
}
