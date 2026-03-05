package cat.company.wandervault.domain.model

import java.time.LocalDate

/**
 * Represents hotel reservation information for a destination stop.
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param destinationId The ID of the [Destination] this hotel belongs to.
 * @param name The name of the hotel.
 * @param address The address of the hotel.
 * @param checkInDate The check-in date.
 * @param checkOutDate The check-out date.
 * @param confirmationNumber The booking confirmation number.
 * @param notes Additional notes about the reservation.
 */
data class Hotel(
    val id: Int = 0,
    val destinationId: Int,
    val name: String = "",
    val address: String = "",
    val checkInDate: LocalDate? = null,
    val checkOutDate: LocalDate? = null,
    val confirmationNumber: String = "",
    val notes: String = "",
)
