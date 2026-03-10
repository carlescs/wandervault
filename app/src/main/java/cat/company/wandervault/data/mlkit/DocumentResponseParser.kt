package cat.company.wandervault.data.mlkit

import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.HotelInfo
import java.time.LocalDate
import java.time.format.DateTimeParseException

private const val SECTION_SEPARATOR = "---"
internal const val FLIGHT_MARKER = "FLIGHT|"
internal const val HOTEL_MARKER = "HOTEL|"

/**
 * Parses the two-section Gemini Nano response into a [DocumentExtractionResult].
 *
 * Expected format:
 * ```
 * <summary text>
 * ---
 * FLIGHT|<airline>|<flight number>|<booking ref>|<departure city>|<arrival city>|<departure date (YYYY-MM-DD)>
 * ```
 * or
 * ```
 * <summary text>
 * ---
 * HOTEL|<hotel name>|<address>|<booking ref>|<check-in (YYYY-MM-DD)>|<check-out (YYYY-MM-DD)>
 * ```
 * or
 * ```
 * <summary text>
 * ---
 * <general trip info text or "None">
 * ```
 *
 * The parser scans **all non-blank lines** of section 2 for a FLIGHT or HOTEL marker, so
 * a model that emits a brief prefix sentence before the structured line (common for
 * non-standard document layouts) still produces a valid extraction. The first matching
 * line is used; subsequent lines are ignored.
 *
 * Section 2 values of "None" (case-insensitive, with optional trailing punctuation) are
 * treated as absent and result in a [DocumentExtractionResult] with no structured info.
 *
 * All pipe-delimited fields are optional: if a field is blank or the model omits it, the
 * corresponding property is `null`.
 */
internal fun parseDocumentResponse(raw: String): DocumentExtractionResult {
    val parts = raw.split(SECTION_SEPARATOR, limit = 2)
    val summary = parts.getOrNull(0)?.trim()?.ifBlank { null } ?: raw.trim()
    val infoRaw = parts.getOrNull(1)?.trim()

    // Treat blank responses or "None" (with optional trailing punctuation) as no info.
    if (infoRaw.isNullOrBlank() ||
        infoRaw.trimEnd('.', '!', ',', ' ').equals("None", ignoreCase = true)
    ) {
        return DocumentExtractionResult(summary = summary)
    }

    // Scan all non-blank lines of section 2 for a structured FLIGHT or HOTEL marker.
    // The model sometimes emits a brief prefix before the marker line, especially for
    // non-standard document layouts, so checking only the first line is insufficient.
    val structuredLine = infoRaw.lines()
        .map { it.trim() }
        .firstOrNull { line ->
            line.startsWith(FLIGHT_MARKER, ignoreCase = true) ||
                line.startsWith(HOTEL_MARKER, ignoreCase = true)
        }

    if (structuredLine != null) {
        if (structuredLine.startsWith(FLIGHT_MARKER, ignoreCase = true)) {
            val fields = structuredLine.substring(FLIGHT_MARKER.length).split("|")
            return DocumentExtractionResult(
                summary = summary,
                flightInfo = FlightInfo(
                    airline = fields.getOrNull(0)?.trim()?.ifBlank { null },
                    flightNumber = fields.getOrNull(1)?.trim()?.ifBlank { null },
                    bookingReference = fields.getOrNull(2)?.trim()?.ifBlank { null },
                    departurePlace = fields.getOrNull(3)?.trim()?.ifBlank { null },
                    arrivalPlace = fields.getOrNull(4)?.trim()?.ifBlank { null },
                    departureDate = fields.getOrNull(5)?.trim()?.ifBlank { null }
                        ?.parseLocalDateOrNull(),
                ),
            )
        }
        if (structuredLine.startsWith(HOTEL_MARKER, ignoreCase = true)) {
            val fields = structuredLine.substring(HOTEL_MARKER.length).split("|")
            return DocumentExtractionResult(
                summary = summary,
                hotelInfo = HotelInfo(
                    name = fields.getOrNull(0)?.trim()?.ifBlank { null },
                    address = fields.getOrNull(1)?.trim()?.ifBlank { null },
                    bookingReference = fields.getOrNull(2)?.trim()?.ifBlank { null },
                    checkInDate = fields.getOrNull(3)?.trim()?.ifBlank { null }
                        ?.parseLocalDateOrNull(),
                    checkOutDate = fields.getOrNull(4)?.trim()?.ifBlank { null }
                        ?.parseLocalDateOrNull(),
                ),
            )
        }
    }

    // Fallback: treat the entire section 2 as general trip-relevant info.
    return DocumentExtractionResult(summary = summary, relevantTripInfo = infoRaw)
}

/**
 * Parses an ISO-8601 date string (YYYY-MM-DD) into a [LocalDate], returning `null` if
 * the string is malformed or represents an invalid date.
 */
internal fun String.parseLocalDateOrNull(): LocalDate? = try {
    LocalDate.parse(this)
} catch (_: DateTimeParseException) {
    null
}
