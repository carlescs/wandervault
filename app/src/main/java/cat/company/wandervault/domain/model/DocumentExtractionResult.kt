package cat.company.wandervault.domain.model

/**
 * Result of extracting information from a travel document using ML Kit.
 *
 * A single document may contain data for multiple trip elements (e.g. a multi-leg flight
 * itinerary or a booking with several hotel stays), so flights and hotels are represented as
 * lists.
 *
 * @param summary A brief AI-generated summary of what the document contains.
 * @param relevantTripInfo General trip-relevant information (dates, destinations, booking
 *   references) when the document contains no structured flight, hotel, or activity lines.
 *   `null` if nothing relevant was found or if [flightInfoList], [hotelInfoList], or
 *   [activityInfoList] is non-empty.
 * @param flightInfoList All structured flight details extracted from the document (one entry per
 *   FLIGHT line in the AI response). Empty for non-flight documents.
 * @param hotelInfoList All structured hotel/accommodation details extracted from the document
 *   (one entry per HOTEL line in the AI response). Empty for non-hotel documents.
 * @param activityInfoList All structured activity/experience details extracted from the document
 *   (one entry per ACTIVITY line in the AI response). Empty for documents with no activity bookings.
 */
data class DocumentExtractionResult(
    val summary: String,
    val relevantTripInfo: String? = null,
    val flightInfoList: List<FlightInfo> = emptyList(),
    val hotelInfoList: List<HotelInfo> = emptyList(),
    val activityInfoList: List<ActivityInfo> = emptyList(),
) {
    /** Convenience accessor: the first extracted flight, or `null` if none were found. */
    val flightInfo: FlightInfo? get() = flightInfoList.firstOrNull()

    /** Convenience accessor: the first extracted hotel, or `null` if none were found. */
    val hotelInfo: HotelInfo? get() = hotelInfoList.firstOrNull()
}
