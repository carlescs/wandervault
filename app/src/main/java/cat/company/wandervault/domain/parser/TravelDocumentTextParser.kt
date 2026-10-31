package cat.company.wandervault.domain.parser

import cat.company.wandervault.domain.model.ScannedDocumentInfo

/**
 * Heuristic parser that extracts travel fields from OCR text of boarding passes, e-tickets and
 * hotel reservations.
 *
 * The parser is deliberately conservative: it prefers values found next to well-known labels
 * (e.g. "Flight", "Booking reference", "PNR") in several languages and only falls back to
 * unlabelled matches when the document clearly looks like a flight document. All results are
 * meant to be reviewed by the user before being saved.
 */
object TravelDocumentTextParser {

    /** Labels that precede a flight number ("Flight", "Flight No.", "Vuelo", "Vol", "Flug", …). */
    private val FLIGHT_LABEL = Regex(
        """\b(?:flight|flt|vuelo|vol|flug|volo|voo|vlucht)\b\.?\s*(?:no\.?|nr\.?|n[º°o]\.?|number|num\.?|#)?\s*:?""",
        RegexOption.IGNORE_CASE,
    )

    /** Words that indicate a flight document even without an explicit flight label. */
    private val FLIGHT_DOCUMENT_HINT = Regex(
        """\b(?:boarding|gate|seat|embarque|embarquement|airlines?|airways|e-?ticket|departure|terminal)\b""",
        RegexOption.IGNORE_CASE,
    )

    /**
     * IATA-style flight number: a two-character airline designator (two letters, or a letter and
     * a digit in either order) followed by 1–4 digits and an optional suffix letter.
     */
    private val FLIGHT_NUMBER = Regex("""\b([A-Z]{2}|[A-Z][0-9]|[0-9][A-Z])\s?([0-9]{1,4}[A-Z]?)\b""")

    /** Upper-case two-letter words that look like airline designators but are not. */
    private val FLIGHT_PREFIX_STOP_WORDS = setOf(
        "NO", "NR", "AM", "PM", "TO", "AT", "ON", "IN", "OF", "BY", "UP", "DE", "LA", "EL", "LE",
    )

    /** Labels that precede a booking / confirmation / PNR code, in several languages. */
    private val BOOKING_LABEL = Regex(
        listOf(
            """booking\s*(?:reference|ref\.?|code|number|no\.?|id)""",
            """confirmation\s*(?:number|code|no\.?|#)?""",
            """reservation\s*(?:number|code|no\.?|#)?""",
            """record\s*locator""",
            """\bpnr\b""",
            """localizador""",
            """c[oó]digo\s*de\s*(?:reserva|confirmaci[oó]n)""",
            """n[uú]mero\s*de\s*(?:reserva|confirmaci[oó]n)""",
            """r[ée]f[ée]rence\s*(?:de\s*)?(?:r[ée]servation)?""",
            """num[ée]ro\s*de\s*(?:r[ée]servation|confirmation)""",
            """buchungs(?:nummer|code)""",
            """best[äa]tigungsnummer""",
            """codice\s*(?:di\s*)?prenotazione""",
            """\bref\b\.?""",
        ).joinToString("|", prefix = "(?:", postfix = ")"),
        RegexOption.IGNORE_CASE,
    )

    /** Upper-case words that commonly follow a booking label but are not codes themselves. */
    private val BOOKING_STOP_WORDS = setOf(
        "NUMBER", "CODE", "REFERENCE", "BOOKING", "STATUS", "CONFIRMED", "CONFIRMATION",
        "RESERVATION", "PASSENGER", "TICKET", "FLIGHT", "PLEASE", "DETAILS", "HOTEL",
    )

    /** Characters that may separate a label from its value. */
    private val VALUE_SEPARATORS = Regex("""[\s:#.,;/()]+""")

    /** A carrier name ending with "Airlines" / "Airways" preceded by up to two capitalised words. */
    private val CARRIER = Regex(
        """((?:\p{Lu}[\p{L}&'.-]*\s+){1,2}(?:Airlines|Airways|Air Lines|AIRLINES|AIRWAYS|AIR LINES))\b""",
    )

    /** Words that identify a line as the name of an accommodation. */
    private val HOTEL_WORD = Regex(
        """\b(?:hotel|hôtel|hostel|resort|inn|suites|apartments?|aparthotel|guest\s*house|b&b|motel|lodge)\b""",
        RegexOption.IGNORE_CASE,
    )

    /** Explicit labels that precede an accommodation name. */
    private val HOTEL_NAME_LABEL = Regex(
        """\b(?:hotel\s*name|property|accommodation|alojamiento|h[ée]bergement|unterkunft)\b\s*:""",
        RegexOption.IGNORE_CASE,
    )

    /** Lines mentioning these words describe the booking rather than naming the hotel. */
    private val HOTEL_LINE_EXCLUSIONS = Regex(
        """\b(?:booking|confirmation|reservation|check[\s-]?in|check[\s-]?out|thank|dear|policy|email|phone|tel)\b|@|www\.|https?:""",
        RegexOption.IGNORE_CASE,
    )

    /** Labels that precede an address. */
    private val ADDRESS_LABEL = Regex(
        """\b(?:address|direcci[oó]n|adresse|indirizzo|endere[cç]o|anschrift)\b\s*:?""",
        RegexOption.IGNORE_CASE,
    )

    private const val MAX_HOTEL_NAME_LENGTH = 60
    private const val MAX_ADDRESS_LENGTH = 120

    /**
     * Parses [text] recognised in a travel document and returns the extracted fields.
     */
    fun parse(text: String): ScannedDocumentInfo {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        return ScannedDocumentInfo(
            rawText = text,
            flightNumber = findFlightNumber(lines),
            bookingReference = findBookingReference(lines),
            carrier = findCarrier(lines),
            hotelName = findHotelName(lines),
            hotelAddress = findHotelAddress(lines),
        )
    }

    // ── Flight number ─────────────────────────────────────────────────────────

    internal fun findFlightNumber(lines: List<String>): String? {
        // 1. A flight number on the same line as a flight label, or on one of the next two lines
        //    (boarding passes often print a header row followed by a value row).
        lines.forEachIndexed { index, line ->
            val label = FLIGHT_LABEL.find(line) ?: return@forEachIndexed
            firstFlightNumber(line.substring(label.range.last + 1))?.let { return it }
            lines.drop(index + 1).take(2).forEach { next -> firstFlightNumber(next)?.let { return it } }
        }
        // 2. No labelled value: only guess when the document clearly is a flight document.
        val looksLikeFlight = lines.any { FLIGHT_LABEL.containsMatchIn(it) || FLIGHT_DOCUMENT_HINT.containsMatchIn(it) }
        if (!looksLikeFlight) return null
        return lines.firstNotNullOfOrNull { line ->
            FLIGHT_NUMBER.findAll(line)
                .firstOrNull { match ->
                    isPlausibleFlightNumber(match) && match.groupValues[2].count { it.isDigit() } >= 2
                }
                ?.let { normalizeFlightNumber(it) }
        }
    }

    private fun firstFlightNumber(text: String): String? =
        FLIGHT_NUMBER.findAll(text).firstOrNull { isPlausibleFlightNumber(it) }?.let { normalizeFlightNumber(it) }

    private fun isPlausibleFlightNumber(match: MatchResult): Boolean =
        match.groupValues[1] !in FLIGHT_PREFIX_STOP_WORDS

    private fun normalizeFlightNumber(match: MatchResult): String =
        match.groupValues[1] + match.groupValues[2]

    // ── Booking reference ─────────────────────────────────────────────────────

    internal fun findBookingReference(lines: List<String>): String? {
        lines.forEachIndexed { index, line ->
            val label = BOOKING_LABEL.find(line) ?: return@forEachIndexed
            firstBookingCode(line.substring(label.range.last + 1))?.let { return it }
            lines.getOrNull(index + 1)?.let { next -> firstBookingCode(next)?.let { return it } }
        }
        return null
    }

    private fun firstBookingCode(text: String): String? =
        text.split(VALUE_SEPARATORS)
            .asSequence()
            .map { it.trim('-') }
            .filter { it.isNotEmpty() }
            .take(3)
            .firstOrNull { isPlausibleBookingCode(it) }

    private fun isPlausibleBookingCode(token: String): Boolean {
        if (token.length !in 5..20) return false
        if (token != token.uppercase()) return false
        if (!token.all { it.isLetterOrDigit() || it == '-' }) return false
        if (token in BOOKING_STOP_WORDS) return false
        val hasDigit = token.any { it.isDigit() }
        val hasLetter = token.any { it.isLetter() }
        // Mixed alphanumerics (e.g. "X7K2PQ"), all-letter 6-char PNRs ("ABCDEF") or long numeric
        // confirmation numbers ("123456789") are accepted.
        return (hasDigit && hasLetter) || (!hasDigit && token.length == 6) || (!hasLetter && token.length >= 6)
    }

    // ── Carrier ───────────────────────────────────────────────────────────────

    internal fun findCarrier(lines: List<String>): String? =
        lines.firstNotNullOfOrNull { line -> CARRIER.find(line)?.groupValues?.get(1)?.trim() }

    // ── Hotel ─────────────────────────────────────────────────────────────────

    internal fun findHotelName(lines: List<String>): String? {
        lines.forEachIndexed { index, line ->
            val label = HOTEL_NAME_LABEL.find(line) ?: return@forEachIndexed
            val value = line.substring(label.range.last + 1).trim().ifEmpty { lines.getOrNull(index + 1)?.trim() }
            if (!value.isNullOrEmpty() && value.length <= MAX_HOTEL_NAME_LENGTH) return value
        }
        return lines.firstOrNull { line ->
            line.length in 3..MAX_HOTEL_NAME_LENGTH &&
                HOTEL_WORD.containsMatchIn(line) &&
                !HOTEL_LINE_EXCLUSIONS.containsMatchIn(line) &&
                !ADDRESS_LABEL.containsMatchIn(line)
        }
    }

    internal fun findHotelAddress(lines: List<String>): String? {
        lines.forEachIndexed { index, line ->
            val label = ADDRESS_LABEL.find(line) ?: return@forEachIndexed
            val value = line.substring(label.range.last + 1).trim().ifEmpty { lines.getOrNull(index + 1)?.trim() }
            if (!value.isNullOrEmpty() && value.length <= MAX_ADDRESS_LENGTH) return value
        }
        return null
    }
}
