package cat.company.wandervault.domain.model

/**
 * Structured hotel/accommodation information extracted from a travel document.
 *
 * @param name The hotel or property name, or `null` if not found.
 * @param address The hotel address, or `null` if not found.
 * @param bookingReference The booking or reservation confirmation code, or `null` if not found.
 */
data class HotelInfo(
    val name: String? = null,
    val address: String? = null,
    val bookingReference: String? = null,
)
