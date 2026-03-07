package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TripDocumentRepository
import kotlinx.coroutines.flow.Flow

/**
 * Returns a live [Flow] of all documents for the trip with the given [tripId].
 */
class GetTripDocumentsUseCase(private val repository: TripDocumentRepository) {
    operator fun invoke(tripId: Int): Flow<List<TripDocument>> =
        repository.getDocumentsForTrip(tripId)
}
