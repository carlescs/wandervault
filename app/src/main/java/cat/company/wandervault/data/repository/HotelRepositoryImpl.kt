package cat.company.wandervault.data.repository

import cat.company.wandervault.data.local.HotelDao
import cat.company.wandervault.data.local.HotelEntity
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.repository.HotelRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HotelRepositoryImpl(private val dao: HotelDao) : HotelRepository {

    override fun getHotelForDestination(destinationId: Int): Flow<Hotel?> =
        dao.getByDestinationId(destinationId).map { it?.toDomain() }

    override suspend fun saveHotel(hotel: Hotel) {
        dao.insert(hotel.toEntity())
    }

    override suspend fun updateHotel(hotel: Hotel) {
        dao.update(hotel.toEntity())
    }

    override suspend fun deleteHotel(hotel: Hotel) {
        dao.delete(hotel.toEntity())
    }
}

internal fun HotelEntity.toDomain() = Hotel(
    id = id,
    destinationId = destinationId,
    name = name,
    address = address,
    checkInDate = checkInDate,
    checkOutDate = checkOutDate,
    confirmationNumber = confirmationNumber,
    notes = notes,
)

private fun Hotel.toEntity() = HotelEntity(
    id = id,
    destinationId = destinationId,
    name = name,
    address = address,
    checkInDate = checkInDate,
    checkOutDate = checkOutDate,
    confirmationNumber = confirmationNumber,
    notes = notes,
)
