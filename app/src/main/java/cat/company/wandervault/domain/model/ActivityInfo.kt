package cat.company.wandervault.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * Structured activity/experience information extracted from a travel document.
 *
 * @param title The activity name or short description (e.g. "Eiffel Tower tour"), or `null` if not found.
 * @param description Additional details about the activity, or `null` if not found.
 * @param date The date of the activity, or `null` if not found.
 * @param time The start time of the activity, or `null` if not found.
 * @param confirmationNumber The booking or confirmation code, or `null` if not found.
 */
data class ActivityInfo(
    val title: String? = null,
    val description: String? = null,
    val date: LocalDate? = null,
    val time: LocalTime? = null,
    val confirmationNumber: String? = null,
)
