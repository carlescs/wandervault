package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.repository.TripDocumentRepository

/** Use-case that updates an existing [TripDocumentFolder]. */
class UpdateFolderUseCase(private val repository: TripDocumentRepository) {
    suspend operator fun invoke(folder: TripDocumentFolder) = repository.updateFolder(folder)
}
