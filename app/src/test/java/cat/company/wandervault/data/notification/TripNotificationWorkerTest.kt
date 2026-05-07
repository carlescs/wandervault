package cat.company.wandervault.data.notification

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.model.activeNotificationNextStep
import cat.company.wandervault.domain.model.hasExpiredNotificationNextStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TripNotificationWorkerTest {

    private val now = ZonedDateTime.of(2026, 5, 7, 12, 0, 0, 0, ZoneOffset.UTC)

    @Test
    fun `activeNotificationNextStep returns trimmed text when deadline is still in the future`() {
        val trip = Trip(
            id = 1,
            title = "Tokyo",
            nextStep = "  Board your train in 20 minutes.  ",
            nextStepDeadline = now.plusMinutes(20),
        )

        assertEquals("Board your train in 20 minutes.", trip.activeNotificationNextStep(now))
        assertFalse(trip.hasExpiredNotificationNextStep(now))
    }

    @Test
    fun `activeNotificationNextStep returns null when deadline has expired`() {
        val trip = Trip(
            id = 1,
            title = "Tokyo",
            nextStep = "Board your train in 20 minutes.",
            nextStepDeadline = now.minusMinutes(1),
        )

        assertNull(trip.activeNotificationNextStep(now))
        assertTrue(trip.hasExpiredNotificationNextStep(now))
    }

    @Test
    fun `activeNotificationNextStep keeps text without a deadline`() {
        val trip = Trip(
            id = 1,
            title = "Tokyo",
            nextStep = "Check hotel reception for late check-in details.",
            nextStepDeadline = null,
        )

        assertEquals(
            "Check hotel reception for late check-in details.",
            trip.activeNotificationNextStep(now),
        )
        assertFalse(trip.hasExpiredNotificationNextStep(now))
    }

    @Test
    fun `blank nextStep is ignored`() {
        val trip = Trip(
            id = 1,
            title = "Tokyo",
            nextStep = "   ",
            nextStepDeadline = now.minusMinutes(1),
        )

        assertNull(trip.activeNotificationNextStep(now))
        assertFalse(trip.hasExpiredNotificationNextStep(now))
    }
}
