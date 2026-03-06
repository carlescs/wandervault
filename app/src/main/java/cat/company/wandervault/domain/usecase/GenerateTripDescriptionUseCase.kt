package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripDescriptionRepository

/**
 * Use-case that generates a short AI description of a trip using on-device Gemini Nano.
 *
 * @return The generated description text, or `null` if Gemini Nano is not available on this device.
 */
class GenerateTripDescriptionUseCase(private val repository: TripDescriptionRepository) {
    suspend operator fun invoke(trip: Trip, destinations: List<Destination>): String? =
        repository.generateDescription(trip, destinations)
}
