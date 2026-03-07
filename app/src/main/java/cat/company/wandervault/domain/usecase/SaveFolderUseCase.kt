package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.repository.TripDocumentRepository

/** Use-case that persists a new [TripDocumentFolder]. */
class SaveFolderUseCase(private val repository: TripDocumentRepository) {
    suspend operator fun invoke(folder: TripDocumentFolder) = repository.saveFolder(folder)
}
