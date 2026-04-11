package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow

/** Use-case that fetches all [Activity] items across every destination of the given trip. */
class GetActivitiesForTripUseCase(private val repository: ActivityRepository) {
    operator fun invoke(tripId: Int): Flow<List<Activity>> =
        repository.getActivitiesForTrip(tripId)
}
