package cat.company.wandervault.data.repository

import cat.company.wandervault.data.local.TripDao
import cat.company.wandervault.data.local.TripEntity
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TripRepositoryImpl(private val dao: TripDao) : TripRepository {

    override fun getTrips(): Flow<List<Trip>> = dao.getAll().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun getTripById(id: Int): Flow<Trip?> = dao.getById(id).map { it?.toDomain() }

    override suspend fun saveTrip(trip: Trip) {
        dao.insert(trip.toEntity())
    }

    override suspend fun updateTrip(trip: Trip) {
        dao.update(trip.toEntity())
    }
}

private fun TripEntity.toDomain() = Trip(
    id = id,
    title = title,
    imageUri = imageUri,
)

private fun Trip.toEntity() = TripEntity(
    id = id,
    title = title,
    imageUri = imageUri,
)
