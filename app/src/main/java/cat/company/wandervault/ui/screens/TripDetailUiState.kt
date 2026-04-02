package cat.company.wandervault.ui.screens

import cat.company.wandervault.domain.model.Trip
import java.time.ZonedDateTime

/**
 * Represents the UI state for the Trip Detail screen.
 */
sealed class TripDetailUiState {
    /** The detail is being loaded. */
    data object Loading : TripDetailUiState()

    /** The trip was loaded successfully. */
    data class Success(
        val trip: Trip,
        val descriptionState: DescriptionState = DescriptionState.None,
        val whatsNextState: WhatsNextState = WhatsNextState.None,
        val upcomingEvents: List<UpcomingEvent> = emptyList(),
    ) : TripDetailUiState()

    /** An error occurred (e.g. trip not found). */
    data object Error : TripDetailUiState()
}

/** Represents the state of the on-device AI trip description shown in the Details tab. */
sealed class DescriptionState {
    /** Description generation is in progress (model may be downloading or generating). */
    data object Loading : DescriptionState()

    /** Description was generated successfully. */
    data class Available(val text: String) : DescriptionState()

    /** Gemini Nano is not supported on this device. */
    data object Unavailable : DescriptionState()

    /** An error occurred during description generation. */
    data object Error : DescriptionState()

    /** The user deleted the description. */
    data object None : DescriptionState()
}

/** Represents the state of the AI-generated "what's next" notice shown in the Details tab. */
sealed class WhatsNextState {
    /** Notice generation is in progress (model may be downloading or generating). */
    data object Loading : WhatsNextState()

    /** Notice was generated successfully. */
    data class Available(val text: String) : WhatsNextState()

    /** Gemini Nano is not supported on this device. */
    data object Unavailable : WhatsNextState()

    /** An error occurred during notice generation. */
    data object Error : WhatsNextState()

    /** Notice has not been generated yet. */
    data object None : WhatsNextState()
}

/**
 * A single upcoming itinerary event shown in the "Next Up" section.
 *
 * @param dateTime The timezone-aware moment when the event occurs.
 * @param destinationName The name of the destination the event is associated with.
 * @param eventType Whether this is an arrival or departure event.
 */
data class UpcomingEvent(
    val dateTime: ZonedDateTime,
    val destinationName: String,
    val eventType: EventType,
) {
    /** The kind of itinerary event. */
    enum class EventType { ARRIVAL, DEPARTURE }
}
