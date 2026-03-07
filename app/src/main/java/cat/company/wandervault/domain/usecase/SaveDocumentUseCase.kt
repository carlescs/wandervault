package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TripDocumentRepository

/** Use-case that persists a new [TripDocument]. */
class SaveDocumentUseCase(private val repository: TripDocumentRepository) {
    suspend operator fun invoke(document: TripDocument) = repository.saveDocument(document)
}
