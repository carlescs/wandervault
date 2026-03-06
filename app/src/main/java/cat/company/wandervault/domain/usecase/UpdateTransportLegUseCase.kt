package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.repository.TransportRepository

/** Use-case that updates an existing [TransportLeg] in the repository. */
class UpdateTransportLegUseCase(private val repository: TransportRepository) {
    suspend operator fun invoke(leg: TransportLeg) = repository.updateTransportLeg(leg)
}
