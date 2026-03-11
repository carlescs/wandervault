package cat.company.wandervault.domain.model

import java.time.LocalDate

/**
 * Structured flight information extracted from a travel document.
 *
 * @param airline The airline name or IATA code (e.g. "Lufthansa", "LH"), or `null` if not found.
 * @param flightNumber The flight number (e.g. "LH1234"), or `null` if not found.
 * @param bookingReference The booking or PNR confirmation code, or `null` if not found.
 * @param departurePlace The departure city or airport name/code, or `null` if not found.
 * @param arrivalPlace The arrival city or airport name/code, or `null` if not found.
 * @param departureDate The scheduled departure date, or `null` if not found. Used as a
 *   secondary matching criterion when the flight number and booking reference are absent
 *   or do not match any existing leg.
 */
data class FlightInfo(
    val airline: String? = null,
    val flightNumber: String? = null,
    val bookingReference: String? = null,
    val departurePlace: String? = null,
    val arrivalPlace: String? = null,
    val departureDate: LocalDate? = null,
)
