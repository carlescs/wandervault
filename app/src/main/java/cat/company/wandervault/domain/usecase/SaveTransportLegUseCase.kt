package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.repository.TransportRepository

/** Use-case that persists a new [TransportLeg] to the repository. */
class SaveTransportLegUseCase(private val repository: TransportRepository) {
    suspend operator fun invoke(leg: TransportLeg) = repository.saveTransportLeg(leg)
}
