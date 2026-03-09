package cat.company.wandervault.data.repository

import cat.company.wandervault.data.local.TransportDao
import cat.company.wandervault.data.local.TransportEntity
import cat.company.wandervault.data.local.TransportLegDao
import cat.company.wandervault.data.local.TransportLegEntity
import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.repository.TransportRepository

class TransportRepositoryImpl(
    private val transportDao: TransportDao,
    private val legDao: TransportLegDao,
) : TransportRepository {

    override suspend fun getOrCreateTransportForDestination(destinationId: Int): Int {
        val existing = transportDao.getByDestinationIdOnce(destinationId)
        if (existing != null) return existing.id
        return transportDao.insert(TransportEntity(destinationId = destinationId)).toInt()
    }

    override suspend fun deleteTransport(transport: Transport) {
        transportDao.delete(transport.toEntity())
    }

    override suspend fun saveTransportLeg(leg: TransportLeg) {
        legDao.insert(leg.toEntity())
    }

    override suspend fun updateTransportLeg(leg: TransportLeg) {
        legDao.update(leg.toEntity())
    }

    override suspend fun deleteTransportLeg(leg: TransportLeg) {
        legDao.delete(leg.toEntity())
    }
}

internal fun TransportEntity.toDomain(legs: List<TransportLeg>) = Transport(
    id = id,
    destinationId = destinationId,
    legs = legs,
)

internal fun TransportLegEntity.toDomain() = TransportLeg(
    id = id,
    transportId = transportId,
    type = runCatching { TransportType.valueOf(type) }.getOrElse { TransportType.OTHER },
    position = position,
    stopName = stopName,
    company = company,
    flightNumber = flightNumber,
    reservationConfirmationNumber = reservationConfirmationNumber,
    isDefault = isDefault,
)

private fun Transport.toEntity() = TransportEntity(
    id = id,
    destinationId = destinationId,
)

private fun TransportLeg.toEntity() = TransportLegEntity(
    id = id,
    transportId = transportId,
    type = type.name,
    position = position,
    stopName = stopName,
    company = company,
    flightNumber = flightNumber,
    reservationConfirmationNumber = reservationConfirmationNumber,
    isDefault = isDefault,
)
