package cat.company.wandervault.domain.model

import java.time.LocalDate

/**
 * Structured hotel/accommodation information extracted from a travel document.
 *
 * @param name The hotel or property name, or `null` if not found.
 * @param address The hotel address, or `null` if not found.
 * @param bookingReference The booking or reservation confirmation code, or `null` if not found.
 * @param checkInDate The check-in date, or `null` if not found.
 * @param checkOutDate The check-out date, or `null` if not found.
 */
data class HotelInfo(
    val name: String? = null,
    val address: String? = null,
    val bookingReference: String? = null,
    val checkInDate: LocalDate? = null,
    val checkOutDate: LocalDate? = null,
)
