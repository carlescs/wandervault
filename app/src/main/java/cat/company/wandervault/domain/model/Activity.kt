package cat.company.wandervault.domain.model

import java.time.ZonedDateTime

/**
 * Represents an activity planned for a destination.
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param destinationId The ID of the destination this activity belongs to.
 * @param title The activity title.
 * @param description An optional description or notes for the activity.
 * @param dateTime The optional date and time of the activity.
 * @param confirmationNumber The optional booking or confirmation code.
 * @param sourceDocumentId The ID of the [cat.company.wandervault.domain.model.TripDocument] from
 *   which this activity's information was extracted, or `null` when entered manually.
 */
data class Activity(
    val id: Int = 0,
    val destinationId: Int,
    val title: String = "",
    val description: String = "",
    val dateTime: ZonedDateTime? = null,
    val confirmationNumber: String = "",
    val sourceDocumentId: Int? = null,
)
