package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Hotel
import java.time.LocalDate

/**
 * UI state for the Location Detail screen.
 *
 * @param destination The destination being viewed. Always non-null once the screen is active.
 */
data class LocationDetailUiState(
    val destination: Destination,
    val hotel: Hotel? = null,
    val isLoading: Boolean = true,
    val hotelName: String = "",
    val hotelAddress: String = "",
    val hotelCheckInDate: LocalDate? = null,
    val hotelCheckOutDate: LocalDate? = null,
    val hotelConfirmationNumber: String = "",
    val hotelNotes: String = "",
    val isHotelFormDirty: Boolean = false,
)
