package cat.company.wandervault.domain.model

/**
 * Structured flight information extracted from a travel document.
 *
 * @param airline The airline name or IATA code (e.g. "Lufthansa", "LH"), or `null` if not found.
 * @param flightNumber The flight number (e.g. "LH1234"), or `null` if not found.
 * @param bookingReference The booking or PNR confirmation code, or `null` if not found.
 * @param departurePlace The departure city or airport name/code, or `null` if not found.
 * @param arrivalPlace The arrival city or airport name/code, or `null` if not found.
 */
data class FlightInfo(
    val airline: String? = null,
    val flightNumber: String? = null,
    val bookingReference: String? = null,
    val departurePlace: String? = null,
    val arrivalPlace: String? = null,
)
