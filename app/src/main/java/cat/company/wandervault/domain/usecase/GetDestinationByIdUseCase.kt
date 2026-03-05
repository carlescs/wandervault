package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.repository.DestinationRepository
import kotlinx.coroutines.flow.Flow

/**
 * Use-case that returns a [Flow] emitting the [Destination] with the given [id],
 * or `null` if no such destination exists.
 */
class GetDestinationByIdUseCase(private val repository: DestinationRepository) {
    operator fun invoke(id: Int): Flow<Destination?> = repository.getDestinationById(id)
}
