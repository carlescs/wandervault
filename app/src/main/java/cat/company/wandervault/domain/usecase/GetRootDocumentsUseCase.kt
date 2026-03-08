package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TripDocumentRepository
import kotlinx.coroutines.flow.Flow

/** Use-case that returns a reactive stream of root-level (no folder) documents for a trip. */
class GetRootDocumentsUseCase(private val repository: TripDocumentRepository) {
    operator fun invoke(tripId: Int): Flow<List<TripDocument>> =
        repository.getRootDocuments(tripId)
}
