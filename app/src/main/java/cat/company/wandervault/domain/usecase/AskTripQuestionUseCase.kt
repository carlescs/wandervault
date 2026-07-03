package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TripDescriptionRepository

/**
 * Use-case that asks on-device Gemini Nano a free-form question about a trip.
 */
class AskTripQuestionUseCase(private val repository: TripDescriptionRepository) {
    suspend fun isAvailable(): Boolean = repository.isAvailable()

    suspend operator fun invoke(
        trip: Trip,
        destinations: List<Destination>,
        activities: List<Activity>,
        hotelsByDestination: Map<Int, Hotel>,
        documents: List<TripDocument>,
        question: String,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)? = null,
    ): String? = repository.askTripQuestion(
        trip = trip,
        destinations = destinations,
        activities = activities,
        hotelsByDestination = hotelsByDestination,
        documents = documents,
        question = question,
        onDownloadProgress = onDownloadProgress,
    )
}
