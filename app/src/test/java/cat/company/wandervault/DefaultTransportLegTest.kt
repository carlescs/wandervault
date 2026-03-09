package cat.company.wandervault

import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.ui.screens.TransportLegEditState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the default-leg selection logic used in
 * [cat.company.wandervault.ui.screens.TransportDetailViewModel.onSetDefaultLeg] and
 * the itinerary timeline icon fallback.
 */
class DefaultTransportLegTest {

    // ── onSetDefaultLeg logic ─────────────────────────────────────────────────

    /**
     * Simulates the core transformation applied by `onSetDefaultLeg(index)`.
     * Extracted here as a pure function so it can be tested without a ViewModel.
     */
    private fun setDefaultLeg(legs: List<TransportLegEditState>, index: Int): List<TransportLegEditState> =
        legs.mapIndexed { i, leg -> leg.copy(isDefault = i == index) }

    @Test
    fun `setDefaultLeg marks the target leg as default`() {
        val legs = listOf(
            TransportLegEditState(id = 1, typeName = TransportType.FLIGHT.name),
            TransportLegEditState(id = 2, typeName = TransportType.TRAIN.name),
            TransportLegEditState(id = 3, typeName = TransportType.BUS.name),
        )

        val result = setDefaultLeg(legs, index = 1)

        assertFalse(result[0].isDefault)
        assertTrue(result[1].isDefault)
        assertFalse(result[2].isDefault)
    }

    @Test
    fun `setDefaultLeg clears the previous default when a new one is chosen`() {
        val legs = listOf(
            TransportLegEditState(id = 1, typeName = TransportType.FLIGHT.name, isDefault = true),
            TransportLegEditState(id = 2, typeName = TransportType.TRAIN.name),
        )

        val result = setDefaultLeg(legs, index = 1)

        assertFalse("previous default should be cleared", result[0].isDefault)
        assertTrue("new default should be set", result[1].isDefault)
    }

    @Test
    fun `setDefaultLeg works correctly for the first leg`() {
        val legs = listOf(
            TransportLegEditState(id = 1, typeName = TransportType.DRIVING.name),
            TransportLegEditState(id = 2, typeName = TransportType.FLIGHT.name),
        )

        val result = setDefaultLeg(legs, index = 0)

        assertTrue(result[0].isDefault)
        assertFalse(result[1].isDefault)
    }

    @Test
    fun `setDefaultLeg works correctly for the last leg`() {
        val legs = listOf(
            TransportLegEditState(id = 1, typeName = TransportType.DRIVING.name),
            TransportLegEditState(id = 2, typeName = TransportType.FLIGHT.name),
        )

        val result = setDefaultLeg(legs, index = 1)

        assertFalse(result[0].isDefault)
        assertTrue(result[1].isDefault)
    }

    // ── timelineLeg() / icon-selection logic ─────────────────────────────────

    /**
     * Simulates `List<TransportLeg>.timelineLeg()` from ItineraryScreen: returns the
     * explicitly-marked default leg, falling back to the first leg when none is set.
     */
    private fun List<TransportLeg>.timelineLeg(): TransportLeg? =
        firstOrNull { it.isDefault } ?: firstOrNull()

    @Test
    fun `timelineLeg returns null for empty list`() {
        assertNull(emptyList<TransportLeg>().timelineLeg())
    }

    @Test
    fun `timelineLeg returns the only leg when there is one`() {
        val leg = TransportLeg(transportId = 1, type = TransportType.FLIGHT)
        assertEquals(leg, listOf(leg).timelineLeg())
    }

    @Test
    fun `timelineLeg falls back to first leg when no default is set`() {
        val flight = TransportLeg(id = 1, transportId = 1, type = TransportType.FLIGHT)
        val train = TransportLeg(id = 2, transportId = 1, type = TransportType.TRAIN)

        assertEquals(flight, listOf(flight, train).timelineLeg())
    }

    @Test
    fun `timelineLeg returns the marked default leg over the first leg`() {
        val flight = TransportLeg(id = 1, transportId = 1, type = TransportType.FLIGHT, isDefault = false)
        val train = TransportLeg(id = 2, transportId = 1, type = TransportType.TRAIN, isDefault = true)

        assertEquals(train, listOf(flight, train).timelineLeg())
    }

    @Test
    fun `timelineLeg handles default set on first leg explicitly`() {
        val flight = TransportLeg(id = 1, transportId = 1, type = TransportType.FLIGHT, isDefault = true)
        val train = TransportLeg(id = 2, transportId = 1, type = TransportType.TRAIN, isDefault = false)

        assertEquals(flight, listOf(flight, train).timelineLeg())
    }
}
