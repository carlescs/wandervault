package cat.company.wandervault

import cat.company.wandervault.data.local.DateConverters
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Unit tests for [DateConverters] ZonedDateTime serialization and backward-compatibility
 * with the legacy LocalDateTime format.
 */
class DateConvertersTest {

    private val converters = DateConverters()

    // ── ZonedDateTime round-trip ──────────────────────────────────────────

    @Test
    fun `fromZonedDateTime returns null for null input`() {
        assertNull(converters.fromZonedDateTime(null))
    }

    @Test
    fun `toZonedDateTime returns null for null input`() {
        assertNull(converters.toZonedDateTime(null))
    }

    @Test
    fun `ZonedDateTime round-trips through string conversion`() {
        val original = ZonedDateTime.of(2024, 6, 15, 10, 30, 0, 0, ZoneId.of("Europe/Paris"))
        val serialized = converters.fromZonedDateTime(original)
        val restored = converters.toZonedDateTime(serialized)

        assertNotNull(serialized)
        assertNotNull(restored)
        // Compare via Instant so that equivalent offsets compare equal regardless of zone ID form.
        assertEquals(original.toInstant(), restored!!.toInstant())
        assertEquals(original.zone, restored.zone)
    }

    @Test
    fun `ZonedDateTime in UTC zone round-trips correctly`() {
        val original = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
        val serialized = converters.fromZonedDateTime(original)
        val restored = converters.toZonedDateTime(serialized)

        assertEquals(original.toInstant(), restored!!.toInstant())
    }

    @Test
    fun `ZonedDateTime across DST boundary preserves wall-clock time`() {
        val parisZone = ZoneId.of("Europe/Paris")
        // Paris transitions from UTC+1 (CET) to UTC+2 (CEST) on the last Sunday of March.
        // 2024-03-31 02:00 is the DST gap; use a time just before the clock springs forward.
        val preDst = ZonedDateTime.of(2024, 3, 30, 23, 30, 0, 0, parisZone) // still UTC+1
        val serialized = converters.fromZonedDateTime(preDst)
        val restored = converters.toZonedDateTime(serialized)

        assertNotNull(restored)
        assertEquals(preDst.toInstant(), restored!!.toInstant())
        assertEquals(preDst.zone, restored.zone)
        // Confirm the offset is UTC+1 (standard time), not UTC+2 (summer time).
        assertEquals(java.time.ZoneOffset.ofHours(1), restored.offset)
    }

    // ── Legacy LocalDateTime backward-compatibility ────────────────────────

    @Test
    fun `toZonedDateTime parses legacy LocalDateTime string without zone`() {
        // Legacy format stored before the ZonedDateTime migration.
        val legacyString = "2024-06-01T12:30:00"
        val result = converters.toZonedDateTime(legacyString)

        assertNotNull(result)
        // The wall-clock values should be preserved.
        assertEquals(2024, result!!.year)
        assertEquals(6, result.monthValue)
        assertEquals(1, result.dayOfMonth)
        assertEquals(12, result.hour)
        assertEquals(30, result.minute)
    }

    @Test
    fun `toZonedDateTime returns null for an unparseable string`() {
        val garbage = "not-a-date"
        val result = converters.toZonedDateTime(garbage)
        assertNull(result)
    }

    // ── LocalDate round-trip (unchanged) ─────────────────────────────────

    @Test
    fun `LocalDate round-trips through string conversion`() {
        val original = LocalDate.of(2024, 12, 31)
        val serialized = converters.fromLocalDate(original)
        val restored = converters.toLocalDate(serialized)

        assertEquals(original, restored)
    }

    @Test
    fun `fromLocalDate returns null for null input`() {
        assertNull(converters.fromLocalDate(null))
    }

    @Test
    fun `toLocalDate returns null for null input`() {
        assertNull(converters.toLocalDate(null))
    }
}
