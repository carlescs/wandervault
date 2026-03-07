package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TripDocumentRepository

/** Use-case that removes a [TripDocument] from the data store. */
class DeleteDocumentUseCase(private val repository: TripDocumentRepository) {
    suspend operator fun invoke(document: TripDocument) = repository.deleteDocument(document)
}
