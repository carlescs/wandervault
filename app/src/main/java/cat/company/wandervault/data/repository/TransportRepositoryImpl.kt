package cat.company.wandervault.data.repository

import cat.company.wandervault.data.local.TransportDao
import cat.company.wandervault.data.local.TransportEntity
import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.repository.TransportRepository

class TransportRepositoryImpl(private val dao: TransportDao) : TransportRepository {

    override suspend fun saveTransport(transport: Transport) {
        dao.insert(transport.toEntity())
    }

    override suspend fun updateTransport(transport: Transport) {
        dao.update(transport.toEntity())
    }

    override suspend fun deleteTransport(transport: Transport) {
        dao.delete(transport.toEntity())
    }
}

internal fun TransportEntity.toDomain() = Transport(
    id = id,
    destinationId = destinationId,
    type = runCatching { TransportType.valueOf(type) }.getOrElse { TransportType.OTHER },
)

private fun Transport.toEntity() = TransportEntity(
    id = id,
    destinationId = destinationId,
    type = type.name,
)
