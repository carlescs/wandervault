package cat.company.wandervault.data.repository

import cat.company.wandervault.data.local.DestinationDao
import cat.company.wandervault.data.local.DestinationEntity
import cat.company.wandervault.data.local.TransportDao
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class DestinationRepositoryImpl(
    private val dao: DestinationDao,
    private val transportDao: TransportDao,
) : DestinationRepository {

    override fun getDestinationById(id: Int): Flow<Destination?> =
        combine(
            dao.getById(id),
            transportDao.getByDestinationId(id),
        ) { entity, transports ->
            entity?.toDomain(transports.map { it.toDomain() })
        }

    override fun getDestinationsForTrip(tripId: Int): Flow<List<Destination>> =
        combine(
            dao.getByTripId(tripId),
            transportDao.getByTripId(tripId),
        ) { destinations, transports ->
            val transportsByDestId = transports.groupBy { it.destinationId }
            destinations.map { it.toDomain(transportsByDestId[it.id]?.map { t -> t.toDomain() } ?: emptyList()) }
        }

    override suspend fun saveDestination(destination: Destination) {
        dao.insert(destination.toEntity())
    }

    override suspend fun updateDestination(destination: Destination) {
        dao.update(destination.toEntity())
    }

    override suspend fun deleteDestination(destination: Destination) {
        dao.delete(destination.toEntity())
    }

    override fun getArrivalTransportForDestination(destinationId: Int): Flow<List<Transport>> =
        transportDao.getArrivalTransportForDestination(destinationId).map { entities ->
            entities.map { it.toDomain() }
        }
}

private fun DestinationEntity.toDomain(transports: List<Transport>) = Destination(
    id = id,
    tripId = tripId,
    name = name,
    position = position,
    arrivalDateTime = arrivalDateTime,
    departureDateTime = departureDateTime,
    transports = transports,
)

private fun Destination.toEntity() = DestinationEntity(
    id = id,
    tripId = tripId,
    name = name,
    position = position,
    arrivalDateTime = arrivalDateTime,
    departureDateTime = departureDateTime,
)
