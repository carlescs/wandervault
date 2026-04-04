package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Transport
import java.time.ZonedDateTime

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
 * Represents the current in-memory editing state for an activity record.
 *
 * @param id Database ID of the existing activity record, or 0 if not yet persisted.
 * @param title The activity title.
 * @param description An optional description or notes.
 * @param dateTime The optional date and time of the activity.
 * @param confirmationNumber The optional booking or confirmation code.
 */
data class ActivityEditState(
    val id: Int = 0,
    val title: String = "",
    val description: String = "",
    val dateTime: ZonedDateTime? = null,
    val confirmationNumber: String = "",
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
        /** The persisted list of activities for this destination. */
        val activities: List<Activity> = emptyList(),
        /** The activity currently being created or edited, or `null` if the form is closed. */
        val activityDraft: ActivityEditState? = null,
    ) : LocationDetailUiState()

    /** An error occurred (e.g. destination not found). */
    data object Error : LocationDetailUiState()
}

