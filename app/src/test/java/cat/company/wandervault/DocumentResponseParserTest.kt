package cat.company.wandervault

import cat.company.wandervault.data.mlkit.parseDocumentResponse
import cat.company.wandervault.domain.model.DocumentExtractionResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for [parseDocumentResponse] — the function that converts raw Gemini Nano output
 * into a structured [DocumentExtractionResult].
 *
 * These tests cover:
 * - Happy-path parsing for FLIGHT and HOTEL documents.
 * - Resilience for non-standard layouts where the model emits a prefix line before the
 *   FLIGHT or HOTEL marker (the parser scans all non-blank lines, not just the first).
 * - "None" variants with and without trailing punctuation.
 * - Missing or blank fields within the structured line.
 * - The new [cat.company.wandervault.domain.model.FlightInfo.departureDate] field (index 5).
 */
class DocumentResponseParserTest {

    // ── FLIGHT document — standard format ─────────────────────────────────────

    @Test
    fun `flight marker on first line parses all fields correctly`() {
        val raw = """
            This is a boarding pass for flight LH1234 from Frankfurt to London.
            ---
            FLIGHT|Lufthansa|LH1234|ABC123|Frankfurt|London|2024-06-15
        """.trimIndent()

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        val fi = result.flightInfo!!
        assertEquals("Lufthansa", fi.airline)
        assertEquals("LH1234", fi.flightNumber)
        assertEquals("ABC123", fi.bookingReference)
        assertEquals("Frankfurt", fi.departurePlace)
        assertEquals("London", fi.arrivalPlace)
        assertEquals(LocalDate.of(2024, 6, 15), fi.departureDate)
        assertNull(result.hotelInfo)
        assertNull(result.relevantTripInfo)
    }

    @Test
    fun `flight marker parsed case-insensitively`() {
        val raw = "Summary.\n---\nflight|Air France|AF001|REF9||Paris\n"

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        assertEquals("Air France", result.flightInfo!!.airline)
        assertEquals("AF001", result.flightInfo!!.flightNumber)
    }

    @Test
    fun `flight marker with missing optional fields produces nulls`() {
        val raw = "Summary.\n---\nFLIGHT|||REF007||\n"

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        val fi = result.flightInfo!!
        assertNull(fi.airline)
        assertNull(fi.flightNumber)
        assertEquals("REF007", fi.bookingReference)
        assertNull(fi.departurePlace)
        assertNull(fi.arrivalPlace)
        assertNull(fi.departureDate)
    }

    @Test
    fun `flight marker without departure date field produces null departureDate`() {
        val raw = "Summary.\n---\nFLIGHT|BA|BA999|BOOK1|Heathrow|JFK\n"

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        assertNull(result.flightInfo!!.departureDate)
        assertEquals("Heathrow", result.flightInfo!!.departurePlace)
        assertEquals("JFK", result.flightInfo!!.arrivalPlace)
    }

    @Test
    fun `flight departure date with invalid format produces null departureDate`() {
        val raw = "Summary.\n---\nFLIGHT|BA|BA999|BOOK1|London|NYC|15-06-2024\n"

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        assertNull(result.flightInfo!!.departureDate)
    }

    // ── FLIGHT document — non-standard layout (marker not on first line) ───────

    @Test
    fun `flight marker detected when model emits prefix text before marker line`() {
        val raw = """
            This document is a flight itinerary.
            ---
            This appears to be a flight booking from Paris to Rome.
            FLIGHT|Alitalia|AZ610|XYZ456|Paris|Rome|2024-08-20
        """.trimIndent()

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        val fi = result.flightInfo!!
        assertEquals("Alitalia", fi.airline)
        assertEquals("AZ610", fi.flightNumber)
        assertEquals(LocalDate.of(2024, 8, 20), fi.departureDate)
        assertNull(result.relevantTripInfo)
    }

    @Test
    fun `flight marker detected even when preceded by multiple blank and non-blank lines`() {
        val raw = "Summary line.\n---\n\nSome preamble text.\nMore text.\nFLIGHT|KLM|KL867|PNR123|Amsterdam|New York\n"

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        assertEquals("KLM", result.flightInfo!!.airline)
        assertEquals("KL867", result.flightInfo!!.flightNumber)
    }

    // ── HOTEL document — standard and non-standard format ─────────────────────

    @Test
    fun `hotel marker on first line parses all fields correctly`() {
        val raw = """
            Hotel booking for Grand Hotel in Paris.
            ---
            HOTEL|Grand Hotel Paris|123 Rue de Rivoli|HTLREF1|2024-09-10|2024-09-14
        """.trimIndent()

        val result = parseDocumentResponse(raw)

        assertNotNull(result.hotelInfo)
        val hi = result.hotelInfo!!
        assertEquals("Grand Hotel Paris", hi.name)
        assertEquals("123 Rue de Rivoli", hi.address)
        assertEquals("HTLREF1", hi.bookingReference)
        assertEquals(LocalDate.of(2024, 9, 10), hi.checkInDate)
        assertEquals(LocalDate.of(2024, 9, 14), hi.checkOutDate)
        assertNull(result.flightInfo)
        assertNull(result.relevantTripInfo)
    }

    @Test
    fun `hotel marker detected when model emits prefix before marker line`() {
        val raw = """
            This is a hotel reservation document.
            ---
            Hotel confirmation for a stay in Barcelona.
            HOTEL|Hotel Arts Barcelona||HOTREF9|2025-05-01|2025-05-05
        """.trimIndent()

        val result = parseDocumentResponse(raw)

        assertNotNull(result.hotelInfo)
        assertEquals("Hotel Arts Barcelona", result.hotelInfo!!.name)
        assertNull(result.hotelInfo!!.address)
        assertEquals(LocalDate.of(2025, 5, 1), result.hotelInfo!!.checkInDate)
    }

    @Test
    fun `hotel marker with invalid check-in date produces null checkInDate`() {
        val raw = "Summary.\n---\nHOTEL|Hotel X||REF1|10/06/2024|12/06/2024\n"

        val result = parseDocumentResponse(raw)

        assertNotNull(result.hotelInfo)
        assertNull(result.hotelInfo!!.checkInDate)
        assertNull(result.hotelInfo!!.checkOutDate)
    }

    // ── "None" and empty section 2 variants ──────────────────────────────────

    @Test
    fun `section 2 of exactly 'None' produces no structured info`() {
        val raw = "Just a receipt summary.\n---\nNone"

        val result = parseDocumentResponse(raw)

        assertNull(result.flightInfo)
        assertNull(result.hotelInfo)
        assertNull(result.relevantTripInfo)
        assertEquals("Just a receipt summary.", result.summary)
    }

    @Test
    fun `section 2 of 'none' (lowercase) produces no structured info`() {
        val raw = "Miscellaneous document.\n---\nnone"

        val result = parseDocumentResponse(raw)

        assertNull(result.flightInfo)
        assertNull(result.hotelInfo)
        assertNull(result.relevantTripInfo)
    }

    @Test
    fun `section 2 of 'None.' (with trailing period) produces no structured info`() {
        val raw = "Summary.\n---\nNone."

        val result = parseDocumentResponse(raw)

        assertNull(result.flightInfo)
        assertNull(result.hotelInfo)
        assertNull(result.relevantTripInfo)
    }

    @Test
    fun `section 2 of 'NONE,' (uppercase with trailing comma) produces no structured info`() {
        val raw = "Summary.\n---\nNONE,"

        val result = parseDocumentResponse(raw)

        assertNull(result.flightInfo)
        assertNull(result.hotelInfo)
        assertNull(result.relevantTripInfo)
    }

    @Test
    fun `blank section 2 produces no structured info`() {
        val raw = "Summary text.\n---\n   \n"

        val result = parseDocumentResponse(raw)

        assertNull(result.flightInfo)
        assertNull(result.hotelInfo)
        assertNull(result.relevantTripInfo)
    }

    @Test
    fun `missing section separator uses entire raw string as summary`() {
        val raw = "No separator here at all."

        val result = parseDocumentResponse(raw)

        assertEquals("No separator here at all.", result.summary)
        assertNull(result.flightInfo)
        assertNull(result.hotelInfo)
    }

    // ── General trip-relevant info fallback ───────────────────────────────────

    @Test
    fun `non-marker section 2 content is returned as relevantTripInfo`() {
        val raw = "Passport scan.\n---\nTravel dates: 2024-07-01 to 2024-07-10. Destination: Tokyo."

        val result = parseDocumentResponse(raw)

        assertNull(result.flightInfo)
        assertNull(result.hotelInfo)
        assertEquals(
            "Travel dates: 2024-07-01 to 2024-07-10. Destination: Tokyo.",
            result.relevantTripInfo,
        )
    }
}
