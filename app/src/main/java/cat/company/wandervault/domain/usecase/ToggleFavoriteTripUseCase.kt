package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripRepository

/** Use-case that atomically toggles the favorite status of a trip at the database level. */
class ToggleFavoriteTripUseCase(private val repository: TripRepository) {
    suspend operator fun invoke(tripId: Int) = repository.toggleFavoriteTrip(tripId)
}
