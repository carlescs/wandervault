package cat.company.wandervault.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class TripNextStepDeadlineTest {

    private val now = ZonedDateTime.of(2026, 5, 7, 12, 0, 0, 0, ZoneOffset.UTC)

    @Test
    fun `returns earliest future destination or activity time`() {
        val destinations = listOf(
            Destination(
                id = 1,
                tripId = 1,
                name = "Rome",
                position = 0,
                arrivalDateTime = now.plusHours(5),
                departureDateTime = now.plusHours(8),
            ),
        )
        val activities = listOf(
            Activity(
                id = 1,
                destinationId = 1,
                title = "Museum",
                dateTime = now.plusHours(2),
            ),
        )

        assertEquals(now.plusHours(2), computeNextStepDeadline(destinations, activities, now))
    }

    @Test
    fun `returns null when no future itinerary times remain`() {
        val destinations = listOf(
            Destination(
                id = 1,
                tripId = 1,
                name = "Rome",
                position = 0,
                arrivalDateTime = now.minusHours(5),
                departureDateTime = now.minusHours(1),
            ),
        )
        val activities = listOf(
            Activity(
                id = 1,
                destinationId = 1,
                title = "Museum",
                dateTime = now.minusMinutes(30),
            ),
        )

        assertNull(computeNextStepDeadline(destinations, activities, now))
    }
}
