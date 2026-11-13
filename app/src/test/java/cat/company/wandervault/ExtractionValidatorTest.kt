package cat.company.wandervault

import cat.company.wandervault.data.mlkit.normalizeForMatching
import cat.company.wandervault.data.mlkit.validateExtraction
import cat.company.wandervault.domain.model.ActivityInfo
import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.HotelInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for [validateExtraction], which drops model-extracted values that are not supported
 * by the source document text.
 */
class ExtractionValidatorTest {

    private val boardingPass = """
        LUFTHANSA BOARDING PASS
        Flight LH 1234  Frankfurt -> London
        15 JUN 2024  Dep 10:30  Arr 12:45
        Booking ref: abc-123
    """.trimIndent()

    private fun flightResult(flight: FlightInfo) =
        DocumentExtractionResult(summary = "Boarding pass", flightInfoList = listOf(flight))

    @Test
    fun `codes present in the source are kept regardless of spacing and case`() {
        val flight = FlightInfo(
            airline = "Lufthansa",
            flightNumber = "LH1234",
            bookingReference = "ABC123",
            departureDate = LocalDate.of(2024, 6, 15),
            departureTime = LocalTime.of(10, 30),
        )

        val result = validateExtraction(flightResult(flight), boardingPass, tripYear = 2024)

        assertEquals(flight, result.flightInfo)
    }

    @Test
    fun `codes missing from the source are dropped`() {
        val flight = FlightInfo(
            airline = "Lufthansa",
            flightNumber = "LH9999",
            bookingReference = "XK7P2Q",
        )

        val result = validateExtraction(flightResult(flight), boardingPass, tripYear = null)

        assertNull(result.flightInfo!!.flightNumber)
        assertNull(result.flightInfo!!.bookingReference)
        assertEquals("Lufthansa", result.flightInfo!!.airline)
    }

    @Test
    fun `values that do not look like flight numbers are dropped`() {
        val source = "Flight 123456 to Paris, airline code AB"
        val result = validateExtraction(
            flightResult(FlightInfo(flightNumber = "123456", arrivalPlace = "Paris")),
            source,
            tripYear = null,
        )

        assertNull(result.flightInfo!!.flightNumber)
        assertEquals("Paris", result.flightInfo!!.arrivalPlace)
    }

    @Test
    fun `three letter airline codes are accepted as flight numbers`() {
        val source = "Vueling VLG8021 Barcelona"
        val result = validateExtraction(
            flightResult(FlightInfo(flightNumber = "VLG8021")),
            source,
            tripYear = null,
        )

        assertEquals("VLG8021", result.flightInfo!!.flightNumber)
    }

    @Test
    fun `dates far from the trip year are dropped`() {
        val flight = FlightInfo(airline = "Lufthansa", departureDate = LocalDate.of(2019, 6, 15))

        val result = validateExtraction(flightResult(flight), boardingPass, tripYear = 2024)

        assertNull(result.flightInfo!!.departureDate)
    }

    @Test
    fun `dates in an adjacent year are kept`() {
        val flight = FlightInfo(airline = "Lufthansa", departureDate = LocalDate.of(2025, 1, 2))

        val result = validateExtraction(flightResult(flight), boardingPass, tripYear = 2024)

        assertEquals(LocalDate.of(2025, 1, 2), result.flightInfo!!.departureDate)
    }

    @Test
    fun `dates are kept when the trip year is unknown`() {
        val flight = FlightInfo(airline = "Lufthansa", departureDate = LocalDate.of(2019, 6, 15))

        val result = validateExtraction(flightResult(flight), boardingPass, tripYear = null)

        assertEquals(LocalDate.of(2019, 6, 15), result.flightInfo!!.departureDate)
    }

    @Test
    fun `check-out on or before check-in is dropped`() {
        val hotel = HotelInfo(
            name = "Hotel Miramar",
            checkInDate = LocalDate.of(2025, 3, 17),
            checkOutDate = LocalDate.of(2025, 3, 14),
        )
        val input = DocumentExtractionResult(summary = "Hotel", hotelInfoList = listOf(hotel))

        val result = validateExtraction(input, "Hotel Miramar", tripYear = 2025)

        assertEquals(LocalDate.of(2025, 3, 17), result.hotelInfo!!.checkInDate)
        assertNull(result.hotelInfo!!.checkOutDate)
    }

    @Test
    fun `hotel booking reference is checked against the source`() {
        val hotel = HotelInfo(name = "Hotel Miramar", bookingReference = "4417820935")
        val input = DocumentExtractionResult(summary = "Hotel", hotelInfoList = listOf(hotel))

        val kept = validateExtraction(input, "Confirmation no. 4417 820 935", tripYear = null)
        val dropped = validateExtraction(input, "Hotel Miramar, Barcelona", tripYear = null)

        assertEquals("4417820935", kept.hotelInfo!!.bookingReference)
        assertNull(dropped.hotelInfo!!.bookingReference)
    }

    @Test
    fun `activity confirmation number is checked against the source`() {
        val activity = ActivityInfo(title = "Sagrada Familia tour", confirmationNumber = "SF-88231")
        val input = DocumentExtractionResult(summary = "Ticket", activityInfoList = listOf(activity))

        val result = validateExtraction(input, "Order SF 88231", tripYear = null)

        assertEquals("SF-88231", result.activityInfoList.single().confirmationNumber)
    }

    @Test
    fun `rows left without any value are removed`() {
        val input = DocumentExtractionResult(
            summary = "Boarding pass",
            flightInfoList = listOf(
                FlightInfo(bookingReference = "NOTREAL"),
                FlightInfo(airline = "Lufthansa"),
            ),
        )

        val result = validateExtraction(input, boardingPass, tripYear = null)

        assertEquals(1, result.flightInfoList.size)
        assertEquals("Lufthansa", result.flightInfo!!.airline)
    }

    @Test
    fun `summary and general info are left untouched`() {
        val input = DocumentExtractionResult(summary = "Travel insurance", relevantTripInfo = "Valid in 2024")

        val result = validateExtraction(input, "anything", tripYear = 2024)

        assertEquals(input, result)
    }

    @Test
    fun `normalizeForMatching strips case accents spaces and punctuation`() {
        assertEquals("SAGRADAFAMILIA88231", normalizeForMatching("Sagrada Família – 88 231!"))
        assertTrue(normalizeForMatching(" - ").isEmpty())
    }
}
