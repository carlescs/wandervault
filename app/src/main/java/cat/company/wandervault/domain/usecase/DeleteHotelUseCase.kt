package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.repository.HotelRepository

/** Use-case that removes a [Hotel] from the repository. */
class DeleteHotelUseCase(private val repository: HotelRepository) {
    suspend operator fun invoke(hotel: Hotel) = repository.deleteHotel(hotel)
}
