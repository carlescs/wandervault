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
        if (hotel.id == 0) {
            dao.insert(hotel.toEntity())
        } else {
            dao.update(hotel.toEntity())
        }
    }

    override suspend fun deleteHotel(hotel: Hotel) {
        dao.delete(hotel.toEntity())
    }
}

private fun HotelEntity.toDomain() = Hotel(
    id = id,
    destinationId = destinationId,
    name = name,
    address = address,
    reservationNumber = reservationNumber,
)

private fun Hotel.toEntity() = HotelEntity(
    id = id,
    destinationId = destinationId,
    name = name,
    address = address,
    reservationNumber = reservationNumber,
)
