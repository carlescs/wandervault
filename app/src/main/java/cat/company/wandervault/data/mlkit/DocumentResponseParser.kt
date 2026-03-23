package cat.company.wandervault.data.mlkit

import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.FolderAssignment
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.OrganizationPlan
import cat.company.wandervault.domain.model.TripDocument
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
 * FLIGHT|<airline>|<flight number>|<booking ref>|<departure city>|<arrival city>|<departure date (YYYY-MM-DD)>
 * ```
 * or
 * ```
 * <summary text>
 * ---
 * HOTEL|<hotel name>|<address>|<booking ref>|<check-in (YYYY-MM-DD)>|<check-out (YYYY-MM-DD)>
 * HOTEL|<hotel name>|<address>|<booking ref>|<check-in (YYYY-MM-DD)>|<check-out (YYYY-MM-DD)>
 * ```
 * or a mix of both, or
 * ```
 * <summary text>
 * ---
 * <general trip info text or "None">
 * ```
 *
 * The parser scans **all non-blank lines** of section 2 for FLIGHT and HOTEL markers, so
 * a model that emits prefix text before the structured lines (common for non-standard document
 * layouts) still produces a valid extraction. **All** matching lines are collected, enabling
 * multi-leg itineraries and multi-hotel bookings to be fully represented.
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

    // Collect ALL non-blank lines from section 2 that start with a structured marker.
    // A document may contain multiple FLIGHT lines (multi-leg itinerary), multiple HOTEL
    // lines (multi-property booking), or a mix of both.
    val nonBlankLines = infoRaw.lines().map { it.trim() }.filter { it.isNotBlank() }

    val flightInfoList = nonBlankLines
        .filter { it.startsWith(FLIGHT_MARKER, ignoreCase = true) }
        .map { line ->
            val fields = line.substring(FLIGHT_MARKER.length).split("|")
            FlightInfo(
                airline = fields.getOrNull(0)?.trim()?.ifBlank { null },
                flightNumber = fields.getOrNull(1)?.trim()?.ifBlank { null },
                bookingReference = fields.getOrNull(2)?.trim()?.ifBlank { null },
                departurePlace = fields.getOrNull(3)?.trim()?.ifBlank { null },
                arrivalPlace = fields.getOrNull(4)?.trim()?.ifBlank { null },
                departureDate = fields.getOrNull(5)?.trim()?.ifBlank { null }
                    ?.parseLocalDateOrNull(),
            )
        }

    val hotelInfoList = nonBlankLines
        .filter { it.startsWith(HOTEL_MARKER, ignoreCase = true) }
        .map { line ->
            val fields = line.substring(HOTEL_MARKER.length).split("|")
            HotelInfo(
                name = fields.getOrNull(0)?.trim()?.ifBlank { null },
                address = fields.getOrNull(1)?.trim()?.ifBlank { null },
                bookingReference = fields.getOrNull(2)?.trim()?.ifBlank { null },
                checkInDate = fields.getOrNull(3)?.trim()?.ifBlank { null }
                    ?.parseLocalDateOrNull(),
                checkOutDate = fields.getOrNull(4)?.trim()?.ifBlank { null }
                    ?.parseLocalDateOrNull(),
            )
        }

    if (flightInfoList.isNotEmpty() || hotelInfoList.isNotEmpty()) {
        return DocumentExtractionResult(
            summary = summary,
            flightInfoList = flightInfoList,
            hotelInfoList = hotelInfoList,
        )
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
