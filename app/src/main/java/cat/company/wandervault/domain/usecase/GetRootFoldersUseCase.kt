package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.repository.TripDocumentRepository
import kotlinx.coroutines.flow.Flow

/** Use-case that returns a reactive stream of root-level folders for a given trip. */
class GetRootFoldersUseCase(private val repository: TripDocumentRepository) {
    operator fun invoke(tripId: Int): Flow<List<TripDocumentFolder>> =
        repository.getRootFolders(tripId)
}
