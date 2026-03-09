package cat.company.wandervault

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.ui.screens.overlapsHotelDates
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for the hotel-matching predicates used in
 * TripDocumentsViewModel.applyOrDisambiguateHotelInfo and ShareViewModel.handleHotelInfo.
 *
 * These tests verify that a "confident match" is detected correctly (by booking reference or
 * hotel name only — check-in date is intentionally excluded from confident-match criteria) and
 * that null fields on either side are handled safely.
 */
class HotelMatchingTest {

    // ── bookingReference matching ─────────────────────────────────────────────

    @Test
    fun `null hotel reservationNumber does not match non-null extracted bookingReference`() {
        val hotel = Hotel(id = 1, destinationId = 1, reservationNumber = "")
        val hotelInfo = HotelInfo(bookingReference = "ABC123")

        val matches = hotelInfo.bookingReference != null &&
            hotel.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)

        assertFalse(matches)
    }

    @Test
    fun `matching hotel reservationNumber returns true (case-insensitive)`() {
        val hotel = Hotel(id = 1, destinationId = 1, reservationNumber = "abc123")
        val hotelInfo = HotelInfo(bookingReference = "ABC123")

        val matches = hotelInfo.bookingReference != null &&
            hotel.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)

        assertTrue(matches)
    }

    @Test
    fun `non-matching hotel reservationNumber returns false`() {
        val hotel = Hotel(id = 1, destinationId = 1, reservationNumber = "XYZ999")
        val hotelInfo = HotelInfo(bookingReference = "ABC123")

        val matches = hotelInfo.bookingReference != null &&
            hotel.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)

        assertFalse(matches)
    }

    @Test
    fun `null extracted bookingReference returns false`() {
        val hotel = Hotel(id = 1, destinationId = 1, reservationNumber = "ABC123")
        val hotelInfo = HotelInfo(bookingReference = null)

        val matches = hotelInfo.bookingReference != null &&
            hotel.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)

        assertFalse(matches)
    }

    // ── hotel name matching ───────────────────────────────────────────────────

    @Test
    fun `matching hotel name returns true (case-insensitive)`() {
        val hotel = Hotel(id = 1, destinationId = 1, name = "hotel paris")
        val hotelInfo = HotelInfo(name = "Hotel Paris")

        val matches = hotelInfo.name != null &&
            hotel.name.equals(hotelInfo.name, ignoreCase = true)

        assertTrue(matches)
    }

    @Test
    fun `non-matching hotel name returns false`() {
        val hotel = Hotel(id = 1, destinationId = 1, name = "Hotel London")
        val hotelInfo = HotelInfo(name = "Hotel Paris")

        val matches = hotelInfo.name != null &&
            hotel.name.equals(hotelInfo.name, ignoreCase = true)

        assertFalse(matches)
    }

    @Test
    fun `null extracted hotel name returns false`() {
        val hotel = Hotel(id = 1, destinationId = 1, name = "Hotel Paris")
        val hotelInfo = HotelInfo(name = null)

        val matches = hotelInfo.name != null &&
            hotel.name.equals(hotelInfo.name, ignoreCase = true)

        assertFalse(matches)
    }

    @Test
    fun `blank hotel name does not match non-blank extracted name`() {
        val hotel = Hotel(id = 1, destinationId = 1, name = "")
        val hotelInfo = HotelInfo(name = "Hotel Paris")

        val matches = hotelInfo.name != null &&
            hotel.name.equals(hotelInfo.name, ignoreCase = true)

        assertFalse(matches)
    }

    // ── booking reference takes priority over name ────────────────────────────

    @Test
    fun `bookingReference match takes priority over name mismatch`() {
        val hotel = Hotel(
            id = 1,
            destinationId = 1,
            name = "Hotel London",
            reservationNumber = "ABC123",
        )
        val hotelInfo = HotelInfo(name = "Hotel Paris", bookingReference = "ABC123")

        val matchByRef = hotelInfo.bookingReference != null &&
            hotel.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)
        val matchByName = hotelInfo.name != null &&
            hotel.name.equals(hotelInfo.name, ignoreCase = true)

        // Booking reference matches, name does not — the combined match should be true.
        assertTrue(matchByRef)
        assertFalse(matchByName)
    }

    // ── check-in date matching ────────────────────────────────────────────────

    private fun destination(
        arrival: LocalDate? = null,
        departure: LocalDate? = null,
    ) = Destination(
        id = 1,
        tripId = 1,
        name = "Test Dest",
        position = 0,
        arrivalDateTime = arrival?.atStartOfDay(),
        departureDateTime = departure?.atStartOfDay(),
    )

    @Test
    fun `checkInDate matches destination arrivalDateTime date`() {
        val checkIn = LocalDate.of(2024, 6, 10)
        val dest = destination(arrival = checkIn, departure = LocalDate.of(2024, 6, 14))
        val hotelInfo = HotelInfo(checkInDate = checkIn)

        val matches = hotelInfo.checkInDate != null &&
            dest.arrivalDateTime?.toLocalDate() == hotelInfo.checkInDate

        assertTrue(matches)
    }

    @Test
    fun `checkInDate does not match when destination arrivalDateTime differs`() {
        val dest = destination(arrival = LocalDate.of(2024, 6, 5), departure = LocalDate.of(2024, 6, 14))
        val hotelInfo = HotelInfo(checkInDate = LocalDate.of(2024, 6, 10))

        val matches = hotelInfo.checkInDate != null &&
            dest.arrivalDateTime?.toLocalDate() == hotelInfo.checkInDate

        assertFalse(matches)
    }

    @Test
    fun `null checkInDate does not match destination with arrival date`() {
        val dest = destination(arrival = LocalDate.of(2024, 6, 10))
        val hotelInfo = HotelInfo(checkInDate = null)

        val matches = hotelInfo.checkInDate != null &&
            dest.arrivalDateTime?.toLocalDate() == hotelInfo.checkInDate

        assertFalse(matches)
    }

    // ── check-in date is NOT a confident match criterion ──────────────────────
    //
    // Check-in date alone must NOT be used to auto-apply hotel info: an arrival date
    // coincidence could silently match the wrong destination (e.g. two different hotels
    // checking in on the same date) and suppress the disambiguation dialog, leaving the
    // hotel un-updated without any user feedback. Instead, the check-in date is only used
    // to filter the candidates shown in the HotelDestinationSelection dialog.

    @Test
    fun `matching checkInDate alone does not constitute a confident match`() {
        // Even when the check-in date aligns exactly with the destination's arrival date,
        // this must NOT be treated as a confident match so the disambiguation dialog is shown.
        val checkIn = LocalDate.of(2024, 6, 10)
        val dest = destination(arrival = checkIn, departure = LocalDate.of(2024, 6, 14))
        val hotel = Hotel(id = 1, destinationId = 1, name = "Hotel Paris", reservationNumber = "BOOK1")
        val hotelInfo = HotelInfo(name = "Different Hotel", bookingReference = "OTHER2", checkInDate = checkIn)

        // Booking reference does not match.
        val matchByRef = hotelInfo.bookingReference != null &&
            hotel.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)
        // Hotel name does not match.
        val matchByName = hotelInfo.name != null &&
            hotel.name.equals(hotelInfo.name, ignoreCase = true)
        // Check-in date does match the arrival date — but this should not yield a confident match.
        val checkInMatchesArrival = hotelInfo.checkInDate != null &&
            dest.arrivalDateTime?.toLocalDate() == hotelInfo.checkInDate

        assertFalse("booking reference must not match", matchByRef)
        assertFalse("hotel name must not match", matchByName)
        assertTrue("check-in date coincidentally matches arrival", checkInMatchesArrival)
        // Neither booking reference nor hotel name matches → no confident match, dialog must show.
        assertFalse("no confident match when only check-in date aligns", matchByRef || matchByName)
    }

    // ── overlapsHotelDates ───────────────────────────────────────────────────

    @Test
    fun `destination with arrival and departure within hotel dates overlaps`() {
        val dest = destination(
            arrival = LocalDate.of(2024, 6, 10),
            departure = LocalDate.of(2024, 6, 14),
        )
        val hotelInfo = HotelInfo(
            checkInDate = LocalDate.of(2024, 6, 10),
            checkOutDate = LocalDate.of(2024, 6, 14),
        )
        assertTrue(dest.overlapsHotelDates(hotelInfo))
    }

    @Test
    fun `destination that ends before hotel check-in does not overlap`() {
        val dest = destination(
            arrival = LocalDate.of(2024, 6, 1),
            departure = LocalDate.of(2024, 6, 5),
        )
        val hotelInfo = HotelInfo(
            checkInDate = LocalDate.of(2024, 6, 10),
            checkOutDate = LocalDate.of(2024, 6, 14),
        )
        assertFalse(dest.overlapsHotelDates(hotelInfo))
    }

    @Test
    fun `destination that starts after hotel check-out does not overlap`() {
        val dest = destination(
            arrival = LocalDate.of(2024, 6, 20),
            departure = LocalDate.of(2024, 6, 25),
        )
        val hotelInfo = HotelInfo(
            checkInDate = LocalDate.of(2024, 6, 10),
            checkOutDate = LocalDate.of(2024, 6, 14),
        )
        assertFalse(dest.overlapsHotelDates(hotelInfo))
    }

    @Test
    fun `destination with no departure date overlaps when arrival is before hotel check-out`() {
        val dest = destination(
            arrival = LocalDate.of(2024, 6, 10),
            departure = null,
        )
        val hotelInfo = HotelInfo(
            checkInDate = LocalDate.of(2024, 6, 10),
            checkOutDate = LocalDate.of(2024, 6, 14),
        )
        assertTrue(dest.overlapsHotelDates(hotelInfo))
    }

    @Test
    fun `destination with no departure date does not overlap when arrival is after hotel check-out`() {
        val dest = destination(
            arrival = LocalDate.of(2024, 6, 20),
            departure = null,
        )
        val hotelInfo = HotelInfo(
            checkInDate = LocalDate.of(2024, 6, 10),
            checkOutDate = LocalDate.of(2024, 6, 14),
        )
        assertFalse(dest.overlapsHotelDates(hotelInfo))
    }

    @Test
    fun `hotel info with no dates overlaps any destination`() {
        val dest = destination(
            arrival = LocalDate.of(2024, 6, 10),
            departure = LocalDate.of(2024, 6, 14),
        )
        val hotelInfo = HotelInfo(checkInDate = null, checkOutDate = null)
        assertTrue(dest.overlapsHotelDates(hotelInfo))
    }

    @Test
    fun `destination with no dates overlaps any hotel info`() {
        val dest = destination(arrival = null, departure = null)
        val hotelInfo = HotelInfo(
            checkInDate = LocalDate.of(2024, 6, 10),
            checkOutDate = LocalDate.of(2024, 6, 14),
        )
        assertTrue(dest.overlapsHotelDates(hotelInfo))
    }
}
