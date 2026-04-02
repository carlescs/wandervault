package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.repository.TripRepository
import kotlinx.coroutines.flow.Flow

/** Use-case that returns a reactive stream of archived trips. */
class GetArchivedTripsUseCase(private val repository: TripRepository) {
    operator fun invoke(): Flow<List<Trip>> = repository.getArchivedTrips()
}
