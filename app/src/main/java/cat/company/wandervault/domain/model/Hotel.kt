package cat.company.wandervault.domain.model

/**
 * Represents the hotel accommodation for a destination.
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param destinationId The ID of the destination this hotel belongs to.
 * @param name The hotel name.
 * @param address The hotel address.
 * @param reservationNumber The booking or reservation confirmation code.
 */
data class Hotel(
    val id: Int = 0,
    val destinationId: Int,
    val name: String = "",
    val address: String = "",
    val reservationNumber: String = "",
)
