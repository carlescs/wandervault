package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TripDocumentRepository

/**
 * Use-case that copies a document from an external URI into the app's internal documents
 * directory and returns the resulting internal file URI string.
 *
 * @return The internal file URI string, or `null` if the copy fails.
 */
class CopyDocumentToInternalStorageUseCase(private val repository: TripDocumentRepository) {
    suspend operator fun invoke(sourceUri: String): String? =
        repository.copyDocumentToInternalStorage(sourceUri)
}
