package cat.company.wandervault.data.repository

import cat.company.wandervault.data.local.DestinationDao
import cat.company.wandervault.data.local.DestinationEntity
import cat.company.wandervault.data.local.TransportDao
import cat.company.wandervault.data.local.TransportLegDao
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class DestinationRepositoryImpl(
    private val dao: DestinationDao,
    private val transportDao: TransportDao,
    private val legDao: TransportLegDao,
) : DestinationRepository {

    override fun getDestinationById(id: Int): Flow<Destination?> =
        transportDao.getByDestinationId(id).flatMapLatest { transportEntity ->
            if (transportEntity == null) {
                dao.getById(id).map { entity -> entity?.toDomain(null) }
            } else {
                combine(
                    dao.getById(id),
                    legDao.getByTransportId(transportEntity.id),
                ) { entity, legs ->
                    entity?.toDomain(transportEntity.toDomain(legs.map { it.toDomain() }))
                }
            }
        }

    override fun getDestinationsForTrip(tripId: Int): Flow<List<Destination>> =
        combine(
            dao.getByTripId(tripId),
            transportDao.getByTripId(tripId),
            legDao.getByTripId(tripId),
        ) { destinations, transports, legs ->
            val legsByTransportId = legs.groupBy { it.transportId }
            val transportByDestId = transports.associateBy { it.destinationId }
            destinations.map { dest ->
                val transportEntity = transportByDestId[dest.id]
                val transport = transportEntity?.let { te ->
                    val transportLegs = legsByTransportId[te.id]?.map { it.toDomain() } ?: emptyList()
                    te.toDomain(transportLegs)
                }
                dest.toDomain(transport)
            }
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

    override fun getArrivalTransportForDestination(destinationId: Int): Flow<Transport?> =
        transportDao.getArrivalTransportForDestination(destinationId).flatMapLatest { transportEntity ->
            if (transportEntity == null) {
                flowOf(null)
            } else {
                legDao.getByTransportId(transportEntity.id).map { legs ->
                    transportEntity.toDomain(legs.map { it.toDomain() })
                }
            }
        }
}

private fun DestinationEntity.toDomain(transport: Transport?) = Destination(
    id = id,
    tripId = tripId,
    name = name,
    position = position,
    arrivalDateTime = arrivalDateTime,
    departureDateTime = departureDateTime,
    transport = transport,
)

private fun Destination.toEntity() = DestinationEntity(
    id = id,
    tripId = tripId,
    name = name,
    position = position,
    arrivalDateTime = arrivalDateTime,
    departureDateTime = departureDateTime,
)
