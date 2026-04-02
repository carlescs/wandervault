package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository
import java.time.ZonedDateTime

/**
 * Use-case that persists an AI-generated "what's next" notice (or clears it) for a trip,
 * together with the deadline after which the notice should be recalculated.
 *
 * @param text The notice text to save, or `null` to clear it.
 * @param deadline The moment after which [text] is considered stale.  Typically set to the
 *   earliest upcoming destination event time.  `null` means the notice has no expiry and will
 *   only be recalculated on itinerary changes or a manual refresh.
 */
class SaveTripWhatsNextUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(trip: Trip, text: String?, deadline: ZonedDateTime?) =
        repository.updateTrip(trip.copy(nextStep = text, nextStepDeadline = deadline))
}
