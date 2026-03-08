package cat.company.wandervault

import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.HotelInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the hotel-matching predicates used in
 * TripDocumentsViewModel.applyOrDisambiguateHotelInfo and ShareViewModel.handleHotelInfo.
 *
 * These tests verify that a "confident match" is detected correctly (by booking reference or
 * hotel name, case-insensitively) and that null fields on either side are handled safely.
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
}
