package cat.company.wandervault.domain.model

/**
 * Result of extracting information from a travel document using ML Kit.
 *
 * @param summary A brief AI-generated summary of what the document contains.
 * @param relevantTripInfo General trip-relevant information (dates, destinations, booking
 *   references) when the document is neither a flight nor hotel document.
 *   `null` if nothing relevant was found or if [flightInfo] or [hotelInfo] is set.
 * @param flightInfo Structured flight details extracted when the document is a boarding pass,
 *   e-ticket, or flight itinerary. `null` for non-flight documents.
 * @param hotelInfo Structured hotel/accommodation details extracted when the document is a hotel
 *   booking confirmation or reservation voucher. `null` for non-hotel documents.
 */
data class DocumentExtractionResult(
    val summary: String,
    val relevantTripInfo: String? = null,
    val flightInfo: FlightInfo? = null,
    val hotelInfo: HotelInfo? = null,
)
