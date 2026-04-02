package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripRepository
import java.time.ZonedDateTime

/**
 * Use-case that persists an AI-generated "what's next" notice (or clears it) for a trip,
 * together with the deadline after which the notice should be recalculated.
 *
 * Only [nextStep] and [nextStepDeadline] are updated; all other trip fields are left unchanged,
 * so concurrent user edits (e.g. title, timezone) made during AI generation are not overwritten.
 *
 * @param tripId The ID of the trip to update.
 * @param text The notice text to save, or `null` to clear it.
 * @param deadline The moment after which [text] is considered stale.  Typically set to the
 *   earliest upcoming destination event time.  `null` means the notice has no expiry and will
 *   only be recalculated on itinerary changes or a manual refresh.
 */
class SaveTripWhatsNextUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(tripId: Int, text: String?, deadline: ZonedDateTime?) =
        repository.updateTripWhatsNext(tripId, text, deadline)
}
