package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.repository.ActivityRepository

/** Use-case that persists or updates an [Activity] record. */
class SaveActivityUseCase(private val repository: ActivityRepository) {
    suspend operator fun invoke(activity: Activity) = repository.saveActivity(activity)
}
