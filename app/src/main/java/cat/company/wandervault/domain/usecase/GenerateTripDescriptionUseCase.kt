package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripDescriptionRepository

/**
 * Use-case that generates a short AI description of a trip using on-device Gemini Nano.
 *
 * The first and last stops in the itinerary are excluded from the summary because they
 * represent the user's hometown rather than travel destinations.
 *
 * @return The generated description text, or `null` if Gemini Nano is not available on this device.
 */
class GenerateTripDescriptionUseCase(private val repository: TripDescriptionRepository) {
    suspend operator fun invoke(trip: Trip, destinations: List<Destination>): String? {
        val travelDestinations = destinations.drop(1).dropLast(1)
        return repository.generateDescription(trip, travelDestinations)
    }
}
