package cat.company.wandervault.domain.model

/**
 * Fields extracted on-device from a scanned travel document (e.g. a boarding pass or a hotel
 * reservation) using ML Kit Text Recognition.
 *
 * All fields are best-effort guesses; they must be reviewed and confirmed by the user before
 * being saved.
 *
 * @param rawText The full text recognised in the image.
 * @param flightNumber A flight / route number such as "VY1234", or `null` if none was found.
 * @param bookingReference A booking, confirmation or PNR code, or `null` if none was found.
 * @param carrier An airline / carrier name such as "British Airways", or `null` if none was found.
 * @param hotelName A hotel / accommodation name, or `null` if none was found.
 * @param hotelAddress A hotel address, or `null` if none was found.
 */
data class ScannedDocumentInfo(
    val rawText: String,
    val flightNumber: String? = null,
    val bookingReference: String? = null,
    val carrier: String? = null,
    val hotelName: String? = null,
    val hotelAddress: String? = null,
) {
    /** `true` when at least one transport-related field was extracted. */
    val hasTransportFields: Boolean
        get() = flightNumber != null || bookingReference != null || carrier != null

    /** `true` when at least one hotel-related field was extracted. */
    val hasHotelFields: Boolean
        get() = hotelName != null || hotelAddress != null || bookingReference != null
}
