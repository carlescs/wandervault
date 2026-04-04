package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.Activity
import kotlinx.coroutines.flow.Flow

/** Repository for managing [Activity] data for destinations. */
interface ActivityRepository {
    /** Returns a [Flow] emitting the list of [Activity] items for the given [destinationId]. */
    fun getActivitiesForDestination(destinationId: Int): Flow<List<Activity>>

    /** Persists or updates the [activity]. */
    suspend fun saveActivity(activity: Activity)

    /** Removes the [activity] from the data store. */
    suspend fun deleteActivity(activity: Activity)
}
