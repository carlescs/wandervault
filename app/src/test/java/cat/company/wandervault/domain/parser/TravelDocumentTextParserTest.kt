package cat.company.wandervault.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [TravelDocumentTextParser].
 */
class TravelDocumentTextParserTest {

    @Test
    fun `extracts labelled flight number and booking reference from a boarding pass`() {
        val text = """
            BOARDING PASS
            PASSENGER DOE/JOHN
            FLIGHT: VY 1234
            GATE B12  SEAT 14C
            BOOKING REFERENCE: X7K2PQ
        """.trimIndent()

        val info = TravelDocumentTextParser.parse(text)

        assertEquals("VY1234", info.flightNumber)
        assertEquals("X7K2PQ", info.bookingReference)
        assertEquals(text, info.rawText)
    }

    @Test
    fun `reads values printed on the line below a header row`() {
        val text = """
            FLIGHT   DATE    SEAT
            BA0456   12MAR   22A
            PNR
            ABCDEF
        """.trimIndent()

        val info = TravelDocumentTextParser.parse(text)

        assertEquals("BA0456", info.flightNumber)
        assertEquals("ABCDEF", info.bookingReference)
    }

    @Test
    fun `does not mistake the No label for an airline code`() {
        val info = TravelDocumentTextParser.parse("FLIGHT NO 2345\nU2 2345")
        assertEquals("U22345", info.flightNumber)
    }

    @Test
    fun `supports labels in other languages`() {
        val text = """
            Tarjeta de embarque
            Vuelo: IB3170
            Localizador: QWE12R
        """.trimIndent()

        val info = TravelDocumentTextParser.parse(text)

        assertEquals("IB3170", info.flightNumber)
        assertEquals("QWE12R", info.bookingReference)
    }

    @Test
    fun `falls back to an unlabelled flight number on flight documents`() {
        val info = TravelDocumentTextParser.parse("E-TICKET\nLH 400 FRA-JFK")
        assertEquals("LH400", info.flightNumber)
    }

    @Test
    fun `does not guess a flight number on non-flight documents`() {
        val info = TravelDocumentTextParser.parse("Hotel Arts\nRoom AB 12")
        assertNull(info.flightNumber)
    }

    @Test
    fun `extracts the carrier name`() {
        val info = TravelDocumentTextParser.parse("Thank you for choosing British Airways\nFlight BA117")
        assertEquals("British Airways", info.carrier)
    }

    @Test
    fun `extracts hotel details from a reservation`() {
        val text = """
            Booking confirmation
            Hotel Arts Barcelona
            Address: Carrer de la Marina 19-21, 08005 Barcelona
            Confirmation number: 123456789
            Check-in: 12 March 2026
        """.trimIndent()

        val info = TravelDocumentTextParser.parse(text)

        assertEquals("Hotel Arts Barcelona", info.hotelName)
        assertEquals("Carrer de la Marina 19-21, 08005 Barcelona", info.hotelAddress)
        assertEquals("123456789", info.bookingReference)
        assertNull(info.flightNumber)
    }

    @Test
    fun `prefers an explicitly labelled property name`() {
        val info = TravelDocumentTextParser.parse("Your stay\nProperty: Casa Bonita\nThe best hotel deals")
        assertEquals("Casa Bonita", info.hotelName)
    }

    @Test
    fun `rejects lower-case words and stop words as booking codes`() {
        val info = TravelDocumentTextParser.parse("Booking confirmation\nDear guest, thank you\nReservation NUMBER")
        assertNull(info.bookingReference)
    }

    @Test
    fun `returns empty fields for unrelated text`() {
        val info = TravelDocumentTextParser.parse("Shopping list\nmilk\neggs")
        assertNull(info.flightNumber)
        assertNull(info.bookingReference)
        assertNull(info.carrier)
        assertNull(info.hotelName)
        assertNull(info.hotelAddress)
    }
}
