package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.repository.ActivityRepository

/** Use-case that removes an [Activity] record from the data store. */
class DeleteActivityUseCase(private val repository: ActivityRepository) {
    suspend operator fun invoke(activity: Activity) = repository.deleteActivity(activity)
}
