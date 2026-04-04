package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow

/** Use-case that fetches all [Activity] items for the given destination. */
class GetActivitiesForDestinationUseCase(private val repository: ActivityRepository) {
    operator fun invoke(destinationId: Int): Flow<List<Activity>> =
        repository.getActivitiesForDestination(destinationId)
}
