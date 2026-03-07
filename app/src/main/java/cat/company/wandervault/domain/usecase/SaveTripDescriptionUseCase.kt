package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository

/**
 * Use-case that persists an AI-generated description (or clears it) for a trip.
 *
 * @param description The description text to save, or `null` to clear it.
 */
class SaveTripDescriptionUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(trip: Trip, description: String?) =
        repository.updateTrip(trip.copy(aiDescription = description))
}
