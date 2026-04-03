package cat.company.wandervault

import cat.company.wandervault.data.mlkit.normalizeSuggestedFilename
import cat.company.wandervault.data.mlkit.parseDocumentResponse
import cat.company.wandervault.data.mlkit.parseOrganizationResponse
import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.TripDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [parseDocumentResponse] — the function that converts raw Gemini Nano output
 * into a structured [DocumentExtractionResult].
 *
 * These tests cover:
 * - Happy-path parsing for FLIGHT and HOTEL documents (single and multiple items).
 * - Resilience for non-standard layouts where the model emits a prefix line before the
 *   FLIGHT or HOTEL marker (the parser scans all non-blank lines).
 * - Mixed documents containing both FLIGHT and HOTEL lines.
 * - "None" variants with and without trailing punctuation.
 * - Missing or blank fields within the structured line.
 * - The [cat.company.wandervault.domain.model.FlightInfo.departureDate] field (index 5).
 * - The [cat.company.wandervault.domain.model.FlightInfo.departureTime] field (index 6).
 * - The [cat.company.wandervault.domain.model.FlightInfo.arrivalTime] field (index 7).
 */
class DocumentResponseParserTest {

    // ── FLIGHT document — standard format ─────────────────────────────────────

    @Test
    fun `flight marker on first line parses all fields correctly`() {
        val raw = """
            This is a boarding pass for flight LH1234 from Frankfurt to London.
            ---
            FLIGHT|Lufthansa|LH1234|ABC123|Frankfurt|London|2024-06-15|10:30|12:45
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
        assertEquals(LocalTime.of(10, 30), fi.departureTime)
        assertEquals(LocalTime.of(12, 45), fi.arrivalTime)
        assertNull(result.hotelInfo)
        assertNull(result.relevantTripInfo)
        assertEquals(1, result.flightInfoList.size)
        assertTrue(result.hotelInfoList.isEmpty())
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
        assertNull(fi.departureTime)
        assertNull(fi.arrivalTime)
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

    // ── FLIGHT document — time fields ─────────────────────────────────────────

    @Test
    fun `flight marker with departure and arrival times parses times correctly`() {
        val raw = "Summary.\n---\nFLIGHT|Iberia|IB3456|PNR99|Madrid|London|2025-07-10|08:15|10:20\n"

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        val fi = result.flightInfo!!
        assertEquals(LocalDate.of(2025, 7, 10), fi.departureDate)
        assertEquals(LocalTime.of(8, 15), fi.departureTime)
        assertEquals(LocalTime.of(10, 20), fi.arrivalTime)
    }

    @Test
    fun `flight marker with departure time only produces null arrivalTime`() {
        val raw = "Summary.\n---\nFLIGHT|Ryanair|FR1234|REF|Dublin|Barcelona|2025-06-01|06:00\n"

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        assertEquals(LocalTime.of(6, 0), result.flightInfo!!.departureTime)
        assertNull(result.flightInfo!!.arrivalTime)
    }

    @Test
    fun `flight marker with blank time fields produces null times`() {
        val raw = "Summary.\n---\nFLIGHT|EasyJet|EZY123|REF|London|Paris|2025-05-01||\n"

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        assertNull(result.flightInfo!!.departureTime)
        assertNull(result.flightInfo!!.arrivalTime)
    }

    @Test
    fun `flight departure time with invalid format produces null departureTime`() {
        val raw = "Summary.\n---\nFLIGHT|BA|BA001|REF|London|NYC|2025-01-01|25:00|10:30\n"

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        assertNull(result.flightInfo!!.departureTime)
        assertEquals(LocalTime.of(10, 30), result.flightInfo!!.arrivalTime)
    }

    @Test
    fun `flight arrival time with invalid format produces null arrivalTime`() {
        val raw = "Summary.\n---\nFLIGHT|BA|BA001|REF|London|NYC|2025-01-01|08:00|99:99\n"

        val result = parseDocumentResponse(raw)

        assertNotNull(result.flightInfo)
        assertEquals(LocalTime.of(8, 0), result.flightInfo!!.departureTime)
        assertNull(result.flightInfo!!.arrivalTime)
    }

    @Test
    fun `multiple flight lines with times all parse correctly`() {
        val raw = """
            Multi-leg itinerary.
            ---
            FLIGHT|Lufthansa|LH1234|ABC|Frankfurt|London|2024-06-15|07:00|08:15
            FLIGHT|British Airways|BA456|ABC|London|New York|2024-06-15|11:00|14:30
        """.trimIndent()

        val result = parseDocumentResponse(raw)

        assertEquals(2, result.flightInfoList.size)
        assertEquals(LocalTime.of(7, 0), result.flightInfoList[0].departureTime)
        assertEquals(LocalTime.of(8, 15), result.flightInfoList[0].arrivalTime)
        assertEquals(LocalTime.of(11, 0), result.flightInfoList[1].departureTime)
        assertEquals(LocalTime.of(14, 30), result.flightInfoList[1].arrivalTime)
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

    // ── Multiple FLIGHT lines ─────────────────────────────────────────────────

    @Test
    fun `multiple flight lines are all parsed into flightInfoList`() {
        val raw = """
            Multi-leg itinerary with two flights.
            ---
            FLIGHT|Lufthansa|LH1234|ABC123|Frankfurt|London|2024-06-15
            FLIGHT|British Airways|BA456|DEF456|London|New York|2024-06-16
        """.trimIndent()

        val result = parseDocumentResponse(raw)

        assertEquals(2, result.flightInfoList.size)
        assertTrue(result.hotelInfoList.isEmpty())
        assertNull(result.relevantTripInfo)

        val first = result.flightInfoList[0]
        assertEquals("Lufthansa", first.airline)
        assertEquals("LH1234", first.flightNumber)
        assertEquals("ABC123", first.bookingReference)
        assertEquals(LocalDate.of(2024, 6, 15), first.departureDate)

        val second = result.flightInfoList[1]
        assertEquals("British Airways", second.airline)
        assertEquals("BA456", second.flightNumber)
        assertEquals("DEF456", second.bookingReference)
        assertEquals(LocalDate.of(2024, 6, 16), second.departureDate)
    }

    @Test
    fun `three flight lines are all parsed into flightInfoList`() {
        val raw = """
            Three-leg booking.
            ---
            FLIGHT|Air France|AF100|REF1|Paris|Frankfurt|2025-03-01
            FLIGHT|Lufthansa|LH200|REF1|Frankfurt|Singapore|2025-03-01
            FLIGHT|Singapore Airlines|SQ300|REF1|Singapore|Sydney|2025-03-02
        """.trimIndent()

        val result = parseDocumentResponse(raw)

        assertEquals(3, result.flightInfoList.size)
        assertEquals("Air France", result.flightInfoList[0].airline)
        assertEquals("Lufthansa", result.flightInfoList[1].airline)
        assertEquals("Singapore Airlines", result.flightInfoList[2].airline)
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
        assertEquals(1, result.hotelInfoList.size)
        assertTrue(result.flightInfoList.isEmpty())
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

    // ── Multiple HOTEL lines ──────────────────────────────────────────────────

    @Test
    fun `multiple hotel lines are all parsed into hotelInfoList`() {
        val raw = """
            Multi-city booking with two hotel stays.
            ---
            HOTEL|Grand Hotel Paris|1 Place Vendome|REFP|2024-09-10|2024-09-14
            HOTEL|Hotel Roma|Via Veneto 9|REFR|2024-09-15|2024-09-18
        """.trimIndent()

        val result = parseDocumentResponse(raw)

        assertEquals(2, result.hotelInfoList.size)
        assertTrue(result.flightInfoList.isEmpty())
        assertNull(result.relevantTripInfo)

        val first = result.hotelInfoList[0]
        assertEquals("Grand Hotel Paris", first.name)
        assertEquals("REFP", first.bookingReference)
        assertEquals(LocalDate.of(2024, 9, 10), first.checkInDate)

        val second = result.hotelInfoList[1]
        assertEquals("Hotel Roma", second.name)
        assertEquals("REFR", second.bookingReference)
        assertEquals(LocalDate.of(2024, 9, 15), second.checkInDate)
    }

    // ── Mixed FLIGHT and HOTEL lines ──────────────────────────────────────────

    @Test
    fun `document with both flight and hotel lines produces both lists`() {
        val raw = """
            Round-trip flight and hotel booking.
            ---
            FLIGHT|Lufthansa|LH1234|ABC123|Frankfurt|London|2024-06-15
            HOTEL|The Savoy|Strand London|HOTELREF|2024-06-15|2024-06-18
        """.trimIndent()

        val result = parseDocumentResponse(raw)

        assertEquals(1, result.flightInfoList.size)
        assertEquals(1, result.hotelInfoList.size)
        assertNull(result.relevantTripInfo)

        assertEquals("Lufthansa", result.flightInfoList[0].airline)
        assertEquals("The Savoy", result.hotelInfoList[0].name)
    }

    @Test
    fun `document with multiple flights and multiple hotels produces complete lists`() {
        val raw = """
            Complex itinerary.
            ---
            FLIGHT|Air France|AF100|REF1|Paris|London|2025-06-01
            FLIGHT|British Airways|BA200|REF1|London|New York|2025-06-02
            HOTEL|London Marriott|Grosvenor Square|REF1|2025-06-01|2025-06-02
            HOTEL|New York Hilton|1335 Ave Americas|REF2|2025-06-02|2025-06-07
        """.trimIndent()

        val result = parseDocumentResponse(raw)

        assertEquals(2, result.flightInfoList.size)
        assertEquals(2, result.hotelInfoList.size)
        assertNull(result.relevantTripInfo)

        assertEquals("Air France", result.flightInfoList[0].airline)
        assertEquals("British Airways", result.flightInfoList[1].airline)
        assertEquals("London Marriott", result.hotelInfoList[0].name)
        assertEquals("New York Hilton", result.hotelInfoList[1].name)
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
        assertTrue(result.flightInfoList.isEmpty())
        assertTrue(result.hotelInfoList.isEmpty())
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
    fun `section 2 of 'None' with trailing period produces no structured info`() {
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

    // ── normalizeSuggestedFilename ─────────────────────────────────────────────

    @Test
    fun `normalizeSuggestedFilename returns clean name for plain input`() {
        assertEquals("Paris Flight Ticket", normalizeSuggestedFilename("Paris Flight Ticket"))
    }

    @Test
    fun `normalizeSuggestedFilename strips surrounding double quotes`() {
        assertEquals("Paris Hotel Booking", normalizeSuggestedFilename("\"Paris Hotel Booking\""))
    }

    @Test
    fun `normalizeSuggestedFilename strips surrounding single quotes`() {
        assertEquals("Rome Trip Invoice", normalizeSuggestedFilename("'Rome Trip Invoice'"))
    }

    @Test
    fun `normalizeSuggestedFilename removes Filename colon prefix`() {
        assertEquals("London Train Ticket", normalizeSuggestedFilename("Filename: London Train Ticket"))
    }

    @Test
    fun `normalizeSuggestedFilename removes filename lowercase colon prefix`() {
        assertEquals("London Train Ticket", normalizeSuggestedFilename("filename: London Train Ticket"))
    }

    @Test
    fun `normalizeSuggestedFilename removes File name colon prefix`() {
        assertEquals("Berlin Bus Pass", normalizeSuggestedFilename("File name: Berlin Bus Pass"))
    }

    @Test
    fun `normalizeSuggestedFilename removes FILENAME uppercase colon prefix`() {
        assertEquals("Tokyo Metro Pass", normalizeSuggestedFilename("FILENAME: Tokyo Metro Pass"))
    }

    @Test
    fun `normalizeSuggestedFilename uses first non-blank line only`() {
        assertEquals("Madrid Hotel Receipt", normalizeSuggestedFilename("\n\nMadrid Hotel Receipt\nSome extra line"))
    }

    @Test
    fun `normalizeSuggestedFilename skips whitespace-only first lines`() {
        assertEquals("Madrid Hotel Receipt", normalizeSuggestedFilename("  \t  \nMadrid Hotel Receipt"))
    }

    @Test
    fun `normalizeSuggestedFilename replaces invalid filename characters with spaces`() {
        assertEquals("A B C D E F G H I", normalizeSuggestedFilename("A/B\\C:D*E?F\"G<H>I"))
    }

    @Test
    fun `normalizeSuggestedFilename collapses consecutive whitespace`() {
        assertEquals("Flight Booking Confirmation", normalizeSuggestedFilename("Flight   Booking  Confirmation"))
    }

    @Test
    fun `normalizeSuggestedFilename returns null for blank input`() {
        assertNull(normalizeSuggestedFilename("   "))
    }

    @Test
    fun `normalizeSuggestedFilename returns null for empty string`() {
        assertNull(normalizeSuggestedFilename(""))
    }

    @Test
    fun `normalizeSuggestedFilename returns null when only invalid chars remain`() {
        assertNull(normalizeSuggestedFilename("///"))
    }

    // ── parseOrganizationResponse ─────────────────────────────────────────────

    private val docA = TripDocument(id = 1, tripId = 1, name = "flight.pdf", uri = "u1", mimeType = "application/pdf")
    private val docB = TripDocument(id = 2, tripId = 1, name = "hotel.pdf", uri = "u2", mimeType = "application/pdf")
    private val docC = TripDocument(id = 3, tripId = 1, name = "insurance.pdf", uri = "u3", mimeType = "application/pdf")

    @Test
    fun `parseOrganizationResponse parses single folder with one document`() {
        val raw = "FOLDER:Flights\nDOC:1"
        val result = parseOrganizationResponse(raw, listOf(docA, docB, docC))

        assertEquals(1, result.folderAssignments.size)
        assertEquals("Flights", result.folderAssignments[0].folderName)
        assertEquals(listOf(docA), result.folderAssignments[0].documents)
    }

    @Test
    fun `parseOrganizationResponse parses multiple folders`() {
        val raw = "FOLDER:Flights\nDOC:1\nFOLDER:Hotels\nDOC:2"
        val result = parseOrganizationResponse(raw, listOf(docA, docB, docC))

        assertEquals(2, result.folderAssignments.size)
        assertEquals("Flights", result.folderAssignments[0].folderName)
        assertEquals(listOf(docA), result.folderAssignments[0].documents)
        assertEquals("Hotels", result.folderAssignments[1].folderName)
        assertEquals(listOf(docB), result.folderAssignments[1].documents)
    }

    @Test
    fun `parseOrganizationResponse parses multiple documents in one folder`() {
        val raw = "FOLDER:Travel Docs\nDOC:1,2,3"
        val result = parseOrganizationResponse(raw, listOf(docA, docB, docC))

        assertEquals(1, result.folderAssignments.size)
        assertEquals(listOf(docA, docB, docC), result.folderAssignments[0].documents)
    }

    @Test
    fun `parseOrganizationResponse returns empty plan for empty response`() {
        val result = parseOrganizationResponse("", listOf(docA, docB))

        assertEquals(0, result.folderAssignments.size)
    }

    @Test
    fun `parseOrganizationResponse ignores out-of-range document indices`() {
        val raw = "FOLDER:Flights\nDOC:1,99"
        val result = parseOrganizationResponse(raw, listOf(docA))

        assertEquals(1, result.folderAssignments.size)
        assertEquals(listOf(docA), result.folderAssignments[0].documents)
    }

    @Test
    fun `parseOrganizationResponse ignores duplicate document indices`() {
        val raw = "FOLDER:Flights\nDOC:1\nFOLDER:Hotels\nDOC:1,2"
        val result = parseOrganizationResponse(raw, listOf(docA, docB))

        // docA assigned to Flights first; duplicate in Hotels is skipped
        assertEquals(2, result.folderAssignments.size)
        assertEquals(listOf(docA), result.folderAssignments[0].documents)
        assertEquals(listOf(docB), result.folderAssignments[1].documents)
    }

    @Test
    fun `parseOrganizationResponse merges duplicate folder names`() {
        val raw = "FOLDER:Flights\nDOC:1\nFOLDER:Flights\nDOC:2"
        val result = parseOrganizationResponse(raw, listOf(docA, docB))

        assertEquals(1, result.folderAssignments.size)
        assertEquals("Flights", result.folderAssignments[0].folderName)
        assertEquals(listOf(docA, docB), result.folderAssignments[0].documents)
    }

    @Test
    fun `parseOrganizationResponse is case-insensitive for markers`() {
        val raw = "folder:Flights\ndoc:1"
        val result = parseOrganizationResponse(raw, listOf(docA))

        assertEquals(1, result.folderAssignments.size)
        assertEquals("Flights", result.folderAssignments[0].folderName)
    }

    @Test
    fun `parseOrganizationResponse skips DOC line with no preceding FOLDER`() {
        val raw = "DOC:1\nFOLDER:Hotels\nDOC:2"
        val result = parseOrganizationResponse(raw, listOf(docA, docB))

        assertEquals(1, result.folderAssignments.size)
        assertEquals("Hotels", result.folderAssignments[0].folderName)
        assertEquals(listOf(docB), result.folderAssignments[0].documents)
    }

    @Test
    fun `parseOrganizationResponse deduplicates repeated indices within the same DOC line`() {
        val raw = "FOLDER:Flights\nDOC:1,1,2"
        val result = parseOrganizationResponse(raw, listOf(docA, docB))

        // docA must appear only once even though its index was repeated
        assertEquals(1, result.folderAssignments.size)
        assertEquals(listOf(docA, docB), result.folderAssignments[0].documents)
    }

    @Test
    fun `parseOrganizationResponse handles DOC indices with trailing period`() {
        val raw = "FOLDER:Flights\nDOC:1.,2."
        val result = parseOrganizationResponse(raw, listOf(docA, docB))

        assertEquals(1, result.folderAssignments.size)
        assertEquals(listOf(docA, docB), result.folderAssignments[0].documents)
    }

    @Test
    fun `parseOrganizationResponse handles DOC indices with trailing annotation`() {
        val raw = "FOLDER:Flights\nDOC:1 (outbound),2 (return)"
        val result = parseOrganizationResponse(raw, listOf(docA, docB))

        assertEquals(1, result.folderAssignments.size)
        assertEquals(listOf(docA, docB), result.folderAssignments[0].documents)
    }
}
