package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripDescriptionRepository
import java.time.ZonedDateTime

/**
 * Use-case that generates a concise "what's next" notice for the current moment in a trip.
 *
 * The notice is generated fresh each time (not persisted) because it depends on the current
 * date and time. All destination arrival/departure datetimes are passed as-is so the AI can
 * take timezones into account. Activities scheduled at each destination are also passed so the
 * model can factor them into the next-step calculation.
 *
 * @return The generated notice text, or `null` if Gemini Nano is not available on this device.
 */
class GenerateWhatsNextUseCase(private val repository: TripDescriptionRepository) {
    suspend operator fun invoke(
        trip: Trip,
        destinations: List<Destination>,
        activities: List<Activity> = emptyList(),
        now: ZonedDateTime = ZonedDateTime.now(),
    ): String? = repository.generateWhatsNext(trip, destinations, now, activities)
}
