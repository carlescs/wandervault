package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TripDocumentRepository
import kotlinx.coroutines.flow.Flow

/** Use-case that returns a reactive stream of documents inside a given folder. */
class GetDocumentsInFolderUseCase(private val repository: TripDocumentRepository) {
    operator fun invoke(folderId: Int): Flow<List<TripDocument>> =
        repository.getDocumentsInFolder(folderId)
}
