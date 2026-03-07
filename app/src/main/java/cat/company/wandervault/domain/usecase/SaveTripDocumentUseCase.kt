package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TripDocumentRepository

/**
 * Saves a [TripDocument] to the repository and returns its auto-generated ID.
 */
class SaveTripDocumentUseCase(private val repository: TripDocumentRepository) {
    suspend operator fun invoke(document: TripDocument): Long =
        repository.saveDocument(document)
}
