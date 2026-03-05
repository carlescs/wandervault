package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.repository.HotelRepository

/** Use-case that updates an existing [Hotel] in the repository. */
class UpdateHotelUseCase(private val repository: HotelRepository) {
    suspend operator fun invoke(hotel: Hotel) = repository.updateHotel(hotel)
}
