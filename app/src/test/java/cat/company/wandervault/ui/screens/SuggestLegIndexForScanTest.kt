package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.ScannedDocumentInfo
import cat.company.wandervault.domain.model.TransportType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [suggestLegIndexForScan].
 */
class SuggestLegIndexForScanTest {

    private val taxi = TransportLegEditState(id = 1, typeName = TransportType.DRIVING.name, company = "Taxi")
    private val emptyFlight = TransportLegEditState(id = 2, typeName = TransportType.FLIGHT.name)
    private val filledFlight = TransportLegEditState(
        id = 3,
        typeName = TransportType.FLIGHT.name,
        flightNumber = "vy 1234",
        confirmationNumber = "X7K2PQ",
    )

    private fun scan(flight: String? = null, booking: String? = null) =
        ScannedDocumentInfo(rawText = "", flightNumber = flight, bookingReference = booking)

    @Test
    fun `no legs suggests a new leg`() {
        assertNull(suggestLegIndexForScan(emptyList(), scan(flight = "VY1234")))
    }

    @Test
    fun `matching flight number wins, ignoring case and spaces`() {
        assertEquals(2, suggestLegIndexForScan(listOf(taxi, emptyFlight, filledFlight), scan(flight = "VY1234")))
    }

    @Test
    fun `matching booking reference is used when the flight number does not match`() {
        assertEquals(2, suggestLegIndexForScan(listOf(taxi, emptyFlight, filledFlight), scan(booking = "x7k2pq")))
    }

    @Test
    fun `first flight leg without a number is suggested for a new flight number`() {
        assertEquals(1, suggestLegIndexForScan(listOf(taxi, emptyFlight, filledFlight), scan(flight = "IB3170")))
    }

    @Test
    fun `falls back to the first leg without references`() {
        assertEquals(0, suggestLegIndexForScan(listOf(taxi, filledFlight), scan(booking = "NEW123")))
    }

    @Test
    fun `suggests a new leg when every leg already has references`() {
        val other = filledFlight.copy(id = 4, flightNumber = "BA1", confirmationNumber = "ZZZ999")
        assertNull(suggestLegIndexForScan(listOf(filledFlight, other), scan(flight = "IB3170")))
    }
}
