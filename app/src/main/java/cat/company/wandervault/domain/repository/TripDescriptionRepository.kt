package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Trip

/**
 * Repository that generates a short AI description for a trip using on-device Gemini Nano.
 */
interface TripDescriptionRepository {

    /**
     * Generates a short description of the trip based on all available trip info.
     *
     * @return The generated description, or `null` if Gemini Nano is not available on this device.
     * @throws Exception if the model is available but generation fails.
     */
    suspend fun generateDescription(trip: Trip, destinations: List<Destination>): String?
}
