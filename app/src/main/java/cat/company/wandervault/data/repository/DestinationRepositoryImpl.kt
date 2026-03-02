package cat.company.wandervault.data.repository

import cat.company.wandervault.data.local.DestinationDao
import cat.company.wandervault.data.local.DestinationEntity
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DestinationRepositoryImpl(private val dao: DestinationDao) : DestinationRepository {

    override fun getDestinationsForTrip(tripId: Int): Flow<List<Destination>> =
        dao.getByTripId(tripId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveDestination(destination: Destination) {
        dao.insert(destination.toEntity())
    }

    override suspend fun updateDestination(destination: Destination) {
        dao.update(destination.toEntity())
    }

    override suspend fun deleteDestination(destination: Destination) {
        dao.delete(destination.toEntity())
    }
}

private fun DestinationEntity.toDomain() = Destination(
    id = id,
    tripId = tripId,
    name = name,
    position = position,
    arrivalDateTime = arrivalDateTime,
    departureDateTime = departureDateTime,
)

private fun Destination.toEntity() = DestinationEntity(
    id = id,
    tripId = tripId,
    name = name,
    position = position,
    arrivalDateTime = arrivalDateTime,
    departureDateTime = departureDateTime,
)
