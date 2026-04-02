package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Trip
import java.time.ZonedDateTime

/**
 * Repository that generates a short AI description for a trip using on-device Gemini Nano.
 */
interface TripDescriptionRepository {

    /**
     * Returns `true` if on-device AI is supported on this device (model is available or can be
     * downloaded). Returns `false` if the device permanently does not support the feature.
     */
    suspend fun isAvailable(): Boolean

    /**
     * Generates a short description of the trip based on all available trip info.
     *
     * @return The generated description, or `null` if Gemini Nano is not available on this device.
     * @throws Exception if the model is available but generation fails.
     */
    suspend fun generateDescription(trip: Trip, destinations: List<Destination>): String?

    /**
     * Generates a concise "what's next" notice for the trip, considering [now] as the current
     * moment and all destination arrival/departure times in their respective timezones.
     *
     * @param now The current date and time (timezone-aware) used to determine the next step.
     * @return The generated notice, or `null` if Gemini Nano is not available on this device.
     * @throws Exception if the model is available but generation fails.
     */
    suspend fun generateWhatsNext(
        trip: Trip,
        destinations: List<Destination>,
        now: ZonedDateTime,
    ): String?
}
