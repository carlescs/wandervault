package cat.company.wandervault.data.mlkit

import cat.company.wandervault.domain.model.ActivityInfo
import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.HotelInfo
import java.text.Normalizer
import java.time.LocalDate
import kotlin.math.abs

/**
 * Flight designator: a 2–3 character airline code containing at least one letter, followed by
 * 1–4 digits and an optional operational suffix letter (e.g. `LH1234`, `U2123`, `VLG8021`).
 * Matched against the [normalizeForMatching] form, so `"LH 1234"` is accepted.
 */
private val FLIGHT_NUMBER_RE = Regex("^(?=[A-Z0-9]{0,2}[A-Z])[A-Z0-9]{2,3}\\d{1,4}[A-Z]?$")

/** Combining diacritical marks, removed after NFD decomposition. */
private val DIACRITICS_RE = Regex("\\p{Mn}+")

/** Largest allowed difference between an extracted date's year and the trip year. */
private const val MAX_YEAR_DISTANCE = 1

/**
 * Checks model-extracted booking data against the document text it was extracted from and drops
 * values that cannot be trusted. Prompting reduces hallucination but cannot prevent it, so this
 * runs on every extraction.
 *
 * Rules:
 * - **Codes must be in the source.** Booking references, flight numbers and confirmation numbers
 *   are dropped unless they appear in [sourceText] (compared ignoring case, accents, spaces and
 *   punctuation, so `"ABC 123"` matches `"abc-123"`). Codes are the values a small model is most
 *   likely to invent, and the ones a traveller relies on verbatim.
 * - **Flight numbers must look like flight numbers** (see [FLIGHT_NUMBER_RE]).
 * - **Dates must be near the trip.** When [tripYear] is known, dates more than
 *   [MAX_YEAR_DISTANCE] year away are dropped.
 * - **Check-out must follow check-in.** Otherwise the check-out date is dropped.
 * - Rows left with no values at all are removed.
 *
 * Names, places and times are not checked: the model legitimately reformats them (airline name
 * from a code, 12-hour → 24-hour times), so a literal match would reject correct values.
 *
 * @param result The parsed extraction.
 * @param sourceText The document text that was given to the model.
 * @param tripYear The year of the trip the document belongs to, or `null` if unknown.
 */
internal fun validateExtraction(
    result: DocumentExtractionResult,
    sourceText: String,
    tripYear: Int?,
): DocumentExtractionResult {
    val source = normalizeForMatching(sourceText)
    fun String?.ifInSource(): String? = this?.takeIf { value ->
        val normalized = normalizeForMatching(value)
        normalized.isNotEmpty() && source.contains(normalized)
    }
    fun LocalDate?.ifNearTrip(): LocalDate? =
        this?.takeIf { tripYear == null || abs(it.year - tripYear) <= MAX_YEAR_DISTANCE }

    val flights = result.flightInfoList.map { flight ->
        flight.copy(
            flightNumber = flight.flightNumber
                ?.takeIf { FLIGHT_NUMBER_RE.matches(normalizeForMatching(it)) }
                .ifInSource(),
            bookingReference = flight.bookingReference.ifInSource(),
            departureDate = flight.departureDate.ifNearTrip(),
        )
    }.filterNot { it.isBlank() }

    val hotels = result.hotelInfoList.map { hotel ->
        val checkIn = hotel.checkInDate.ifNearTrip()
        val checkOut = hotel.checkOutDate.ifNearTrip()
            ?.takeIf { checkIn == null || it.isAfter(checkIn) }
        hotel.copy(
            bookingReference = hotel.bookingReference.ifInSource(),
            checkInDate = checkIn,
            checkOutDate = checkOut,
        )
    }.filterNot { it.isBlank() }

    val activities = result.activityInfoList.map { activity ->
        activity.copy(
            confirmationNumber = activity.confirmationNumber.ifInSource(),
            date = activity.date.ifNearTrip(),
        )
    }.filterNot { it.isBlank() }

    return result.copy(
        flightInfoList = flights,
        hotelInfoList = hotels,
        activityInfoList = activities,
    )
}

/**
 * Upper-cases [text] and strips accents and every character that is not a letter or digit, so
 * that values can be compared regardless of OCR spacing, punctuation and case.
 */
internal fun normalizeForMatching(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(DIACRITICS_RE, "")
        .uppercase()
        .filter { it.isLetterOrDigit() }

private fun FlightInfo.isBlank(): Boolean = listOf(
    airline, flightNumber, bookingReference, departurePlace, arrivalPlace,
    departureDate, departureTime, arrivalTime,
).all { it == null }

private fun HotelInfo.isBlank(): Boolean =
    listOf(name, address, bookingReference, checkInDate, checkOutDate).all { it == null }

private fun ActivityInfo.isBlank(): Boolean =
    listOf(title, description, date, time, confirmationNumber).all { it == null }
