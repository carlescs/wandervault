package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.repository.HotelRepository

/** Use-case that persists or updates a [Hotel] record. */
class SaveHotelUseCase(private val repository: HotelRepository) {
    suspend operator fun invoke(hotel: Hotel) = repository.saveHotel(hotel)
}
