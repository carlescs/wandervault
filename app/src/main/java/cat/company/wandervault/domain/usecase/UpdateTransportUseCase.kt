package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.repository.TransportRepository

/**
 * Use-case that updates an existing transport leg in the repository.
 *
 * This use-case is kept for backward API compatibility.  Prefer
 * [UpdateTransportLegUseCase] in new code.
 */
@Deprecated("Use UpdateTransportLegUseCase instead")
class UpdateTransportUseCase(private val repository: TransportRepository) {
    suspend operator fun invoke(leg: TransportLeg) = repository.updateTransportLeg(leg)
}
