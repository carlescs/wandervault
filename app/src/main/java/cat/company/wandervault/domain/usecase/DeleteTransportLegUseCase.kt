package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.repository.TransportRepository

/** Use-case that removes a [TransportLeg] from the repository. */
class DeleteTransportLegUseCase(private val repository: TransportRepository) {
    suspend operator fun invoke(leg: TransportLeg) = repository.deleteTransportLeg(leg)
}
