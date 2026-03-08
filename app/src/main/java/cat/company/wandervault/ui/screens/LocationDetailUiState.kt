package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Transport

/**
 * Represents the current in-memory editing state for a hotel record.
 *
 * @param id Database ID of the existing hotel record, or 0 if not yet persisted.
 * @param name The hotel name.
 * @param address The hotel address.
 * @param reservationNumber The booking or reservation confirmation code.
 */
data class HotelEditState(
    val id: Int = 0,
    val name: String = "",
    val address: String = "",
    val reservationNumber: String = "",
)

/**
 * Represents the UI state for the Location Detail screen.
 */
sealed class LocationDetailUiState {
    /** The destination details are being loaded. */
    data object Loading : LocationDetailUiState()

    /** The destination was loaded successfully. */
    data class Success(
        val destination: Destination,
        /** The transport (with its legs) used to arrive at this destination (from the preceding stop). */
        val arrivalTransport: Transport? = null,
        /** `true` when this is the first (start) destination in the trip; no arrival transport applies. */
        val isFirst: Boolean = false,
        /** `true` when this is the last (end) destination in the trip; no departure transport applies. */
        val isLast: Boolean = false,
        /** The current (possibly dirty) hotel editing state. */
        val hotelEditState: HotelEditState = HotelEditState(),
        /** The current (possibly dirty) notes text. */
        val notes: String = "",
        /**
         * The current state of an in-progress or completed document scan, or `null` when no scan
         * is active. Call [LocationDetailViewModel.dismissScan] to dismiss.
         */
        val scanState: DocumentScanUiState? = null,
    ) : LocationDetailUiState()

    /** An error occurred (e.g. destination not found). */
    data object Error : LocationDetailUiState()
}

