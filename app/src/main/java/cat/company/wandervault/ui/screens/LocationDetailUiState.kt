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
 * @param sourceDocumentId The ID of the document from which this hotel's information was
 *   extracted, or `null` when entered manually.
 * @param sourceDocumentName The display name of the source document, or `null` when not sourced
 *   from a document or the document could not be resolved.
 */
data class HotelEditState(
    val id: Int = 0,
    val name: String = "",
    val address: String = "",
    val reservationNumber: String = "",
    val sourceDocumentId: Int? = null,
    val sourceDocumentName: String? = null,
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
 * State of the on-device translation of a destination's notes.
 */
sealed class NotesTranslationState {
    /** No translation has been requested (or it was dismissed). */
    data object Idle : NotesTranslationState()

    /** A translation is in progress (this may include downloading the language model). */
    data object Translating : NotesTranslationState()

    /**
     * The notes were translated successfully.
     *
     * @param text The translated notes.
     * @param sourceLanguage BCP-47 tag of the original notes language.
     * @param targetLanguage BCP-47 tag of the translation language.
     */
    data class Translated(
        val text: String,
        val sourceLanguage: String,
        val targetLanguage: String,
    ) : NotesTranslationState()

    /** The detected language pair cannot be translated on this device. */
    data object Unsupported : NotesTranslationState()

    /** The translation failed (e.g. the language model could not be downloaded). */
    data object Error : NotesTranslationState()
}

/**
 * Language detection and translation state for the destination notes.
 *
 * @param detectedLanguage BCP-47 tag of the language the notes are written in, or `null` when
 *   the notes are empty/too short or the language could not be determined.
 * @param targetLanguage BCP-47 tag of the language the notes would be translated into (the
 *   user's AI output language, or the device language).
 * @param canTranslate `true` when [detectedLanguage] differs from [targetLanguage] and both are
 *   supported by on-device translation.
 * @param detectionFailed `true` when language identification failed with an error.
 * @param translation The current translation state.
 */
data class NotesLanguageState(
    val detectedLanguage: String? = null,
    val targetLanguage: String? = null,
    val canTranslate: Boolean = false,
    val detectionFailed: Boolean = false,
    val translation: NotesTranslationState = NotesTranslationState.Idle,
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
        /** Language detection and translation state for [notes]. */
        val notesLanguage: NotesLanguageState = NotesLanguageState(),
        /** The persisted list of activities for this destination. */
        val activities: List<Activity> = emptyList(),
        /** The activity currently being created or edited, or `null` if the form is closed. */
        val activityDraft: ActivityEditState? = null,
    ) : LocationDetailUiState()

    /** An error occurred (e.g. destination not found). */
    data object Error : LocationDetailUiState()
}

