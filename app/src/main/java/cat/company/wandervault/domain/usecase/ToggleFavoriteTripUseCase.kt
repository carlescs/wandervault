package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository

/** Use-case that toggles the favorite status of a [Trip]. */
class ToggleFavoriteTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(trip: Trip) =
        repository.updateTrip(trip.copy(isFavorite = !trip.isFavorite))
}
