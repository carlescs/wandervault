package cat.company.wandervault.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

/**
 * Unit tests for the [formatted] and [formattedWithDays] Duration extension functions.
 */
class DurationUtilsTest {

    // --- formatted() ---

    @Test
    fun `formatted - minutes only`() {
        assertEquals("45m", Duration.ofMinutes(45).formatted())
    }

    @Test
    fun `formatted - hours only`() {
        assertEquals("2h", Duration.ofHours(2).formatted())
    }

    @Test
    fun `formatted - hours and minutes`() {
        assertEquals("2h 35m", Duration.ofHours(2).plusMinutes(35).formatted())
    }

    @Test
    fun `formatted - more than 24 hours shows total hours`() {
        assertEquals("25h 30m", Duration.ofHours(25).plusMinutes(30).formatted())
    }

    @Test
    fun `formatted - zero minutes`() {
        assertEquals("0m", Duration.ZERO.formatted())
    }

    // --- formattedWithDays() ---

    @Test
    fun `formattedWithDays - minutes only`() {
        assertEquals("45m", Duration.ofMinutes(45).formattedWithDays())
    }

    @Test
    fun `formattedWithDays - hours only`() {
        assertEquals("2h", Duration.ofHours(2).formattedWithDays())
    }

    @Test
    fun `formattedWithDays - hours and minutes`() {
        assertEquals("2h 35m", Duration.ofHours(2).plusMinutes(35).formattedWithDays())
    }

    @Test
    fun `formattedWithDays - days only`() {
        assertEquals("3d", Duration.ofDays(3).formattedWithDays())
    }

    @Test
    fun `formattedWithDays - days and hours`() {
        assertEquals("1d 2h", Duration.ofDays(1).plusHours(2).formattedWithDays())
    }

    @Test
    fun `formattedWithDays - days and minutes`() {
        assertEquals("1d 30m", Duration.ofDays(1).plusMinutes(30).formattedWithDays())
    }

    @Test
    fun `formattedWithDays - days hours and minutes`() {
        assertEquals("2d 3h 30m", Duration.ofDays(2).plusHours(3).plusMinutes(30).formattedWithDays())
    }

    @Test
    fun `formattedWithDays - more than 24 hours shows days`() {
        assertEquals("1d 1h 30m", Duration.ofHours(25).plusMinutes(30).formattedWithDays())
    }

    @Test
    fun `formattedWithDays - zero minutes`() {
        assertEquals("0m", Duration.ZERO.formattedWithDays())
    }
}
