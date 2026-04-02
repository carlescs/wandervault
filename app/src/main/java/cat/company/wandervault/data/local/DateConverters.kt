package cat.company.wandervault.data.local

import androidx.room.TypeConverter
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class DateConverters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    /**
     * Serialises a [ZonedDateTime] as an ISO-8601 string that includes full zone information
     * (e.g. `2024-06-01T12:00:00+02:00[Europe/Paris]`).
     */
    @TypeConverter
    fun fromZonedDateTime(dateTime: ZonedDateTime?): String? = dateTime?.toString()

    /**
     * Deserialises a stored string to a [ZonedDateTime].
     *
     * Handles two formats for backward compatibility:
     * 1. ISO_ZONED_DATE_TIME (new format): `2024-06-01T12:00:00+02:00[Europe/Paris]`
     * 2. Legacy ISO_LOCAL_DATE_TIME (old format, no zone): `2024-06-01T12:00:00`
     *    Treated as the device's default timezone on read.
     */
    @TypeConverter
    fun toZonedDateTime(value: String?): ZonedDateTime? = value?.let {
        try {
            ZonedDateTime.parse(it)
        } catch (_: Exception) {
            try {
                LocalDateTime.parse(it).atZone(ZoneId.systemDefault())
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Serialises a [List] of strings as a pipe-delimited (`|`) string so that it can be stored in
     * a single SQLite TEXT column.  An empty list is stored as an empty string.
     *
     * **Safety**: Firebase UIDs consist of alphanumeric characters only (no pipes), so `|` is a
     * safe delimiter for this specific use-case.
     */
    @TypeConverter
    fun fromStringList(list: List<String>): String = list.joinToString("|")

    /**
     * Deserialises a pipe-delimited string back into a [List] of strings.  An empty or blank
     * input string returns an empty list.
     */
    @TypeConverter
    fun toStringList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|")
}
