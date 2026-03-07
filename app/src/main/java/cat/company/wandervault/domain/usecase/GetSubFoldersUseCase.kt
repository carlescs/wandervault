package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.repository.TripDocumentRepository
import kotlinx.coroutines.flow.Flow

/** Use-case that returns a reactive stream of direct sub-folders of a given parent folder. */
class GetSubFoldersUseCase(private val repository: TripDocumentRepository) {
    operator fun invoke(parentFolderId: Int): Flow<List<TripDocumentFolder>> =
        repository.getSubFolders(parentFolderId)
}
