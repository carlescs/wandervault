package cat.company.wandervault.data.repository

import cat.company.wandervault.data.local.ActivityDao
import cat.company.wandervault.data.local.ActivityEntity
import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.repository.ActivityRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ActivityRepositoryImpl(private val dao: ActivityDao) : ActivityRepository {

    override fun getActivitiesForDestination(destinationId: Int): Flow<List<Activity>> =
        dao.getByDestinationId(destinationId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveActivity(activity: Activity) {
        if (activity.id == 0) {
            dao.insert(activity.toEntity())
        } else {
            dao.update(activity.toEntity())
        }
    }

    override suspend fun deleteActivity(activity: Activity) {
        dao.delete(activity.toEntity())
    }
}

private fun ActivityEntity.toDomain() = Activity(
    id = id,
    destinationId = destinationId,
    title = title,
    description = description,
    dateTime = dateTime,
    confirmationNumber = confirmationNumber,
)

private fun Activity.toEntity() = ActivityEntity(
    id = id,
    destinationId = destinationId,
    title = title,
    description = description,
    dateTime = dateTime,
    confirmationNumber = confirmationNumber,
)
