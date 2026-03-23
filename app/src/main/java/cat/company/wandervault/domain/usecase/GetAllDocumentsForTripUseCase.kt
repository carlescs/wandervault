package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TripDocumentRepository
import kotlinx.coroutines.flow.Flow

/** Use-case that returns a reactive stream of all documents (at any depth) for a given trip. */
class GetAllDocumentsForTripUseCase(private val repository: TripDocumentRepository) {
    operator fun invoke(tripId: Int): Flow<List<TripDocument>> =
        repository.getAllDocumentsForTrip(tripId)
}
