package cat.company.wandervault.data.mlkit

import cat.company.wandervault.domain.model.ActivityInfo
import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.FolderAssignment
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.OrganizationPlan
import cat.company.wandervault.domain.model.TripDocument
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeParseException

private const val SECTION_SEPARATOR = "---"
internal const val FLIGHT_MARKER = "FLIGHT|"
internal const val HOTEL_MARKER = "HOTEL|"
internal const val ACTIVITY_MARKER = "ACTIVITY|"

/**
 * Parses the two-section Gemini Nano response into a [DocumentExtractionResult].
 *
 * Expected format:
 * ```
 * <summary text>
 * ---
 * FLIGHT|<airline>|<flight number>|<booking ref>|<departure city>|<arrival city>|<departure date (YYYY-MM-DD)>|<departure time (HH:MM)>|<arrival time (HH:MM)>
 * FLIGHT|<airline>|<flight number>|<booking ref>|<departure city>|<arrival city>|<departure date (YYYY-MM-DD)>|<departure time (HH:MM)>|<arrival time (HH:MM)>
 * ```
 * or
 * ```
 * <summary text>
 * ---
 * HOTEL|<hotel name>|<address>|<booking ref>|<check-in (YYYY-MM-DD)>|<check-out (YYYY-MM-DD)>
 * HOTEL|<hotel name>|<address>|<booking ref>|<check-in (YYYY-MM-DD)>|<check-out (YYYY-MM-DD)>
 * ```
 * or
 * ```
 * <summary text>
 * ---
 * ACTIVITY|<title>|<description>|<date (YYYY-MM-DD)>|<time (HH:MM)>|<confirmation number>
 * ACTIVITY|<title>|<description>|<date (YYYY-MM-DD)>|<time (HH:MM)>|<confirmation number>
 * ```
 * or a mix of any of the above, or
 * ```
 * <summary text>
 * ---
 * <general trip info text or "None">
 * ```
 *
 * The parser scans **all non-blank lines** of section 2 for FLIGHT, HOTEL and ACTIVITY markers,
 * so a model that emits prefix text before the structured lines (common for non-standard document
 * layouts) still produces a valid extraction. **All** matching lines are collected, enabling
 * multi-leg itineraries, multi-hotel bookings and multi-activity documents to be fully represented.
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
    return buildExtractionResult(summary, parts.getOrNull(1))
}

/**
 * Combines a [summary] with the structured/general info text [infoRaw] into a
 * [DocumentExtractionResult].
 *
 * - Blank or "None" (case-insensitive, optional trailing punctuation) [infoRaw] → no info.
 * - Any FLIGHT/HOTEL/ACTIVITY marker lines → structured lists (see [parseStructuredInfo]).
 * - Otherwise the whole of [infoRaw] becomes [DocumentExtractionResult.relevantTripInfo].
 */
internal fun buildExtractionResult(summary: String, infoRaw: String?): DocumentExtractionResult {
    val info = infoRaw?.trim()
    if (info.isNullOrBlank() || info.isNoneAnswer()) {
        return DocumentExtractionResult(summary = summary)
    }
    val structured = parseStructuredInfo(info)
    if (!structured.isEmpty) {
        return DocumentExtractionResult(
            summary = summary,
            flightInfoList = structured.flights,
            hotelInfoList = structured.hotels,
            activityInfoList = structured.activities,
        )
    }
    // Fallback: treat the entire text as general trip-relevant info.
    return DocumentExtractionResult(summary = summary, relevantTripInfo = info)
}

/** Returns `true` when the model answered "None" (with optional trailing punctuation). */
private fun String.isNoneAnswer(): Boolean =
    trimEnd('.', '!', ',', ' ').equals("None", ignoreCase = true)

/**
 * Structured booking rows parsed from model output by [parseStructuredInfo].
 */
internal data class StructuredInfo(
    val flights: List<FlightInfo> = emptyList(),
    val hotels: List<HotelInfo> = emptyList(),
    val activities: List<ActivityInfo> = emptyList(),
) {
    /** `true` when no rows of any kind were found. */
    val isEmpty: Boolean get() = flights.isEmpty() && hotels.isEmpty() && activities.isEmpty()
}

/**
 * Collects **all** non-blank lines of [raw] that start with a FLIGHT, HOTEL or ACTIVITY marker
 * and parses their pipe-delimited fields. Lines without a marker (model preamble, commentary) are
 * ignored, so multi-leg itineraries, multi-hotel bookings and mixed documents are all supported.
 * Blank or unparsable fields become `null`.
 */
internal fun parseStructuredInfo(raw: String): StructuredInfo {
    val nonBlankLines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }

    val flightInfoList = nonBlankLines
        .filter { it.startsWith(FLIGHT_MARKER, ignoreCase = true) }
        .map { line ->
            val fields = line.substring(FLIGHT_MARKER.length).split("|")
            FlightInfo(
                airline = fields.field(0),
                flightNumber = fields.field(1),
                bookingReference = fields.field(2),
                departurePlace = fields.field(3),
                arrivalPlace = fields.field(4),
                departureDate = fields.field(5)?.parseLocalDateOrNull(),
                departureTime = fields.field(6)?.parseLocalTimeOrNull(),
                arrivalTime = fields.field(7)?.parseLocalTimeOrNull(),
            )
        }

    val hotelInfoList = nonBlankLines
        .filter { it.startsWith(HOTEL_MARKER, ignoreCase = true) }
        .map { line ->
            val fields = line.substring(HOTEL_MARKER.length).split("|")
            HotelInfo(
                name = fields.field(0),
                address = fields.field(1),
                bookingReference = fields.field(2),
                checkInDate = fields.field(3)?.parseLocalDateOrNull(),
                checkOutDate = fields.field(4)?.parseLocalDateOrNull(),
            )
        }

    val activityInfoList = nonBlankLines
        .filter { it.startsWith(ACTIVITY_MARKER, ignoreCase = true) }
        .map { line ->
            val fields = line.substring(ACTIVITY_MARKER.length).split("|")
            ActivityInfo(
                title = fields.field(0),
                description = fields.field(1),
                date = fields.field(2)?.parseLocalDateOrNull(),
                time = fields.field(3)?.parseLocalTimeOrNull(),
                confirmationNumber = fields.field(4),
            )
        }

    return StructuredInfo(flightInfoList, hotelInfoList, activityInfoList)
}

/** Returns the trimmed field at [index], or `null` when it is missing or blank. */
private fun List<String>.field(index: Int): String? = getOrNull(index)?.trim()?.ifBlank { null }

/**
 * Kinds of booking the extraction pipeline can turn into structured rows.
 *
 * @property marker The line prefix used for this kind in model output.
 */
internal enum class StructuredDocumentType(val marker: String) {
    FLIGHT(FLIGHT_MARKER),
    HOTEL(HOTEL_MARKER),
    ACTIVITY(ACTIVITY_MARKER),
}

private val DOCUMENT_TYPE_RE = Regex("\\b(FLIGHT|HOTEL|ACTIVITY)\\b", RegexOption.IGNORE_CASE)

/**
 * Parses the classification answer (e.g. `"FLIGHT, HOTEL"`) into the set of booking kinds it
 * names, in a stable order. Unknown words and `OTHER` are ignored, so an empty set means the
 * document has no structured bookings.
 */
internal fun parseDocumentTypes(raw: String): Set<StructuredDocumentType> {
    val found = DOCUMENT_TYPE_RE.findAll(raw)
        .map { StructuredDocumentType.valueOf(it.value.uppercase()) }
        .toSet()
    return StructuredDocumentType.entries.filterTo(linkedSetOf<StructuredDocumentType>()) { it in found }
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

/**
 * Parses an HH:MM time string into a [LocalTime], returning `null` if the string is malformed
 * or represents an invalid time.
 */
internal fun String.parseLocalTimeOrNull(): LocalTime? = try {
    LocalTime.parse(this)
} catch (_: DateTimeParseException) {
    null
}

/**
 * Cleans raw Gemini Nano output into a safe, single-line filename string.
 *
 * - Takes only the first non-blank line (models sometimes add preamble or newlines).
 * - Strips surrounding quotation marks and common model preamble patterns (e.g. "Filename:").
 * - Replaces characters that are invalid in file names (`/`, `\`, `:`, `*`, `?`, `"`, `<`,
 *   `>`, `|`) with spaces.
 * - Collapses consecutive spaces and trims the result.
 * - Returns `null` if nothing meaningful remains after cleaning.
 */
internal fun normalizeSuggestedFilename(raw: String): String? {
    val firstLine = raw.lineSequence().firstOrNull { it.isNotBlank() } ?: return null
    // Strip surrounding quotes (single or double)
    val unquoted = firstLine.trim().removeSurrounding("\"").removeSurrounding("'").trim()
    // Remove common preamble patterns the model sometimes adds (case-insensitive, single pass)
    val withoutPreamble = unquoted.replace(Regex("^(?:file ?name|filename)\\s*:\\s*", RegexOption.IGNORE_CASE), "").trim()
    // Replace characters that are illegal in filenames
    val sanitized = withoutPreamble.replace(Regex("[/\\\\:*?\"<>|]"), " ")
    // Collapse consecutive whitespace and trim
    val collapsed = sanitized.replace(Regex("\\s+"), " ").trim()
    return collapsed.takeIf { it.isNotBlank() }
}

private const val FOLDER_MARKER = "FOLDER:"
private const val DOC_MARKER = "DOC:"

/**
 * Matches a run of one or more digits at the start of a string.
 * Used to extract document indices robustly — the model sometimes appends punctuation
 * or short annotations after a number (e.g. "1." or "2 (note)").
 */
private val LEADING_INT_RE = Regex("^\\d+")

/**
 * Parses the Gemini Nano auto-organize response into an [OrganizationPlan].
 *
 * Expected format (one or more folder blocks):
 * ```
 * FOLDER:<folder name>
 * DOC:<comma-separated 1-based document numbers>
 * ```
 *
 * - Folder names and document assignments are accumulated in the order they appear.
 * - If the model emits the same folder name twice, documents from both blocks are merged.
 * - Document indices that are out of range or already assigned are silently skipped.
 * - Lines not matching the expected format are ignored gracefully.
 *
 * @param raw Raw text produced by Gemini Nano.
 * @param documents The ordered list of documents that were passed to the model (1-based index).
 */
internal fun parseOrganizationResponse(
    raw: String,
    documents: List<TripDocument>,
): OrganizationPlan {
    // LinkedHashMap to accumulate docs per folder name while preserving insertion order.
    val folderDocs = linkedMapOf<String, MutableList<TripDocument>>()
    var currentFolderName: String? = null
    val assignedDocIndices = mutableSetOf<Int>()

    for (line in raw.lines()) {
        val trimmed = line.trim()
        when {
            trimmed.startsWith(FOLDER_MARKER, ignoreCase = true) -> {
                currentFolderName = trimmed.substring(FOLDER_MARKER.length).trim().ifBlank { null }
            }
            trimmed.startsWith(DOC_MARKER, ignoreCase = true) -> {
                val folderName = currentFolderName ?: continue
                val validIndices = trimmed.substring(DOC_MARKER.length)
                    .split(",")
                    .mapNotNull { token ->
                        // Extract the leading integer, ignoring any trailing punctuation or
                        // annotation the model may append (e.g. "1." or "2 (note)").
                        LEADING_INT_RE.find(token.trim())?.value?.toIntOrNull()
                    }
                    .distinct()
                    .filter { it in 1..documents.size && it !in assignedDocIndices }
                assignedDocIndices += validIndices
                val docs = validIndices.map { documents[it - 1] }
                if (docs.isNotEmpty()) {
                    folderDocs.getOrPut(folderName) { mutableListOf() }.addAll(docs)
                }
            }
        }
    }

    val assignments = folderDocs.map { (name, docs) -> FolderAssignment(name, docs) }
    return OrganizationPlan(folderAssignments = assignments)
}
