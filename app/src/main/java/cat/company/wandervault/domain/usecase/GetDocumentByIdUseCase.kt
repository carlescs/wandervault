package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TripDocumentRepository
import kotlinx.coroutines.flow.Flow

/** Use-case that returns a reactive stream for the [TripDocument] with the given [id]. */
class GetDocumentByIdUseCase(private val repository: TripDocumentRepository) {
    operator fun invoke(id: Int): Flow<TripDocument?> = repository.getDocumentById(id)
}
