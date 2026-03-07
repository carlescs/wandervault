package cat.company.wandervault.domain.model

import java.time.LocalDate

/**
 * Represents a trip.
 *
 * @param startDate The earliest date derived from the trip's itinerary, or `null` if no
 *   destinations with dates have been added yet.
 * @param endDate The latest date derived from the trip's itinerary, or `null` if no
 *   destinations with dates have been added yet.
 */
data class Trip(
    val id: Int,
    val title: String,
    val imageUri: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val aiDescription: String? = null,
    val isFavorite: Boolean = false,
)
