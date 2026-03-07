package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TripDocumentRepository

/**
 * Deletes a [TripDocument] and its associated file from internal storage.
 */
class DeleteTripDocumentUseCase(private val repository: TripDocumentRepository) {
    suspend operator fun invoke(document: TripDocument) =
        repository.deleteDocument(document)
}
