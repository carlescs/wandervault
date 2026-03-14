package cat.company.wandervault.domain.model

import java.time.LocalDate

/**
 * Represents a trip.
 *
 * @param startDate The earliest date derived from the trip's itinerary, or `null` if no
 *   destinations with dates have been added yet.
 * @param endDate The latest date derived from the trip's itinerary, or `null` if no
 *   destinations with dates have been added yet.
 * @param defaultTimezone IANA timezone ID (e.g. `"Europe/Paris"`) used as the default when
 *   creating new destinations and legs in this trip.  `null` means the device's system default
 *   timezone is used.
 */
data class Trip(
    val id: Int,
    val title: String,
    val imageUri: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val aiDescription: String? = null,
    val isFavorite: Boolean = false,
    val defaultTimezone: String? = null,
)
