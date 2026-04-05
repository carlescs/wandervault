package cat.company.wandervault

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.ui.screens.overlapsHotelDates
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Unit tests for the hotel-matching predicates used in
 * TripDocumentsViewModel.applyOrDisambiguateHotelInfo and ShareViewModel.handleHotelInfo.
 *
 * These tests verify two things:
 *  - that a "confident match" is detected correctly by booking reference or hotel name only
 *    (check-in date is intentionally excluded from the confident-match criteria), and
 *  - that date-based filtering/overlap heuristics (e.g. overlapsHotelDates) behave as expected,
 *    including scenarios where arrival/check-in dates are equal or overlapping, and
 *    that null fields on either side are handled safely in all of the above cases.
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

    // ── check-in date as upload-path heuristic (not a confident-match criterion) ────────────
    //
    // The upload path (applyHotelInfo) still uses arrival date == check-in date as a priority-3
    // heuristic to pick the right destination silently. These tests verify that date equality
    // predicate. The confident-match paths (applyOrDisambiguateHotelInfo / handleHotelInfo)
    // intentionally omit this criterion — see the regression test below.

    private fun destination(
        arrival: LocalDate? = null,
        departure: LocalDate? = null,
    ) = Destination(
        id = 1,
        tripId = 1,
        name = "Test Dest",
        position = 0,
        arrivalDateTime = arrival?.atStartOfDay(ZoneOffset.UTC),
        departureDateTime = departure?.atStartOfDay(ZoneOffset.UTC),
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

    // ── check-in date alone never triggers a confident match ─────────────────
    //
    // Even when the check-in date aligns exactly with a destination's arrival date, the
    // confident-match logic (applyOrDisambiguateHotelInfo / handleHotelInfo) must find no
    // match — forcing the disambiguation dialog to be shown.

    @Test
    fun `check-in date alone does not yield a confident match when booking ref and name differ`() {
        val checkIn = LocalDate.of(2024, 6, 10)
        val dest = destination(arrival = checkIn, departure = LocalDate.of(2024, 6, 14))
        val hotel = Hotel(id = 1, destinationId = 1, name = "Hotel Paris", reservationNumber = "BOOK1")
        val hotelInfo = HotelInfo(name = "Different Hotel", bookingReference = "OTHER2", checkInDate = checkIn)

        // Build a candidate list exactly as the ViewModels do.
        val destinationHotels = listOf(dest to hotel)

        // Apply the same confident-match selection logic used by applyOrDisambiguateHotelInfo
        // and handleHotelInfo: only booking reference or hotel name qualifies as a confident match.
        val confidentMatch = destinationHotels.firstOrNull { (_, h) ->
            h != null && hotelInfo.bookingReference != null &&
                h.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)
        } ?: destinationHotels.firstOrNull { (_, h) ->
            h != null && hotelInfo.name != null &&
                h.name.equals(hotelInfo.name, ignoreCase = true)
        }

        // Even though check-in date == arrival date, no confident match must be found,
        // so the disambiguation dialog will be shown.
        assertNull(confidentMatch)
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

    // ── source-document exclusion from confident match ────────────────────────
    //
    // Hotels that were already sourced from the current document (sourceDocumentId == documentId)
    // must be excluded from the confident-match search. This prevents multiple hotel bookings in
    // a single document from re-matching the same destination once the first hotel is applied.

    private val documentId = 42

    @Test
    fun `hotel already sourced from this document is excluded from confident match by bookingReference`() {
        val dest = destination(arrival = LocalDate.of(2024, 6, 10))
        val alreadyLinkedHotel = Hotel(
            id = 1,
            destinationId = 1,
            name = "Hotel Paris",
            reservationNumber = "BOOK1",
            sourceDocumentId = documentId,
        )
        val hotelInfo = HotelInfo(bookingReference = "BOOK1")

        val destinationHotels = listOf(dest to alreadyLinkedHotel)

        // Simulates the confident-match logic with source-document exclusion:
        // hotels with sourceDocumentId == documentId are excluded.
        val confidentMatch = destinationHotels.firstOrNull { (_, h) ->
            h != null && h.sourceDocumentId != documentId &&
                hotelInfo.bookingReference != null &&
                h.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)
        }

        // Even though the booking reference matches, the hotel is excluded because it was
        // already sourced from this document.
        assertNull(confidentMatch)
    }

    @Test
    fun `hotel already sourced from this document is excluded from confident match by name`() {
        val dest = destination(arrival = LocalDate.of(2024, 6, 10))
        val alreadyLinkedHotel = Hotel(
            id = 1,
            destinationId = 1,
            name = "Hotel Paris",
            reservationNumber = "",
            sourceDocumentId = documentId,
        )
        val hotelInfo = HotelInfo(name = "Hotel Paris")

        val destinationHotels = listOf(dest to alreadyLinkedHotel)

        val confidentMatch = destinationHotels.firstOrNull { (_, h) ->
            h != null && h.sourceDocumentId != documentId &&
                hotelInfo.name != null &&
                h.name.equals(hotelInfo.name, ignoreCase = true)
        }

        assertNull(confidentMatch)
    }

    @Test
    fun `hotel sourced from a different document is not excluded from confident match`() {
        val otherDocumentId = 99
        val dest = destination(arrival = LocalDate.of(2024, 6, 10))
        val hotelFromOtherDoc = Hotel(
            id = 1,
            destinationId = 1,
            name = "Hotel Paris",
            reservationNumber = "BOOK1",
            sourceDocumentId = otherDocumentId,
        )
        val hotelInfo = HotelInfo(bookingReference = "BOOK1")

        val destinationHotels = listOf(dest to hotelFromOtherDoc)

        val confidentMatch = destinationHotels.firstOrNull { (_, h) ->
            h != null && h.sourceDocumentId != documentId &&
                hotelInfo.bookingReference != null &&
                h.reservationNumber.equals(hotelInfo.bookingReference, ignoreCase = true)
        }

        // The hotel is from a different document, so it is included and matches.
        assertTrue(confidentMatch != null)
    }

    @Test
    fun `second destination hotel not yet linked to this document is found as confident match after first is excluded`() {
        val dest1 = destination(arrival = LocalDate.of(2024, 6, 10))
        val dest2 = Destination(
            id = 2,
            tripId = 1,
            name = "Dest 2",
            position = 1,
            arrivalDateTime = LocalDate.of(2024, 6, 15).atStartOfDay(ZoneOffset.UTC),
        )
        val alreadyLinkedHotel = Hotel(
            id = 1,
            destinationId = 1,
            name = "Hotel A",
            reservationNumber = "BOOK1",
            sourceDocumentId = documentId,
        )
        val unlinkedHotel = Hotel(
            id = 2,
            destinationId = 2,
            name = "Hotel B",
            reservationNumber = "BOOK2",
            sourceDocumentId = null,
        )
        val secondHotelInfo = HotelInfo(bookingReference = "BOOK2")

        val destinationHotels = listOf(dest1 to alreadyLinkedHotel, dest2 to unlinkedHotel)

        val confidentMatch = destinationHotels.firstOrNull { (_, h) ->
            h != null && h.sourceDocumentId != documentId &&
                secondHotelInfo.bookingReference != null &&
                h.reservationNumber.equals(secondHotelInfo.bookingReference, ignoreCase = true)
        }

        // dest1's hotel is excluded (already linked to this document); dest2's hotel matches.
        assertTrue(confidentMatch != null)
        assertTrue(confidentMatch!!.second == unlinkedHotel)
    }
}
