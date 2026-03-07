package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.repository.TripDocumentRepository

/** Use-case that removes a [TripDocumentFolder] and all its contents. */
class DeleteFolderUseCase(private val repository: TripDocumentRepository) {
    suspend operator fun invoke(folder: TripDocumentFolder) = repository.deleteFolder(folder)
}
