package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.repository.TripDocumentRepository
import kotlinx.coroutines.flow.Flow

/** Use-case that returns a reactive stream of all folders (at any depth) for a given trip. */
class GetAllFoldersForTripUseCase(private val repository: TripDocumentRepository) {
    operator fun invoke(tripId: Int): Flow<List<TripDocumentFolder>> =
        repository.getAllFoldersForTrip(tripId)
}
