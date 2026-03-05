package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.repository.TransportRepository

/** Use-case that updates an existing [Transport] in the repository. */
class UpdateTransportUseCase(private val repository: TransportRepository) {
    suspend operator fun invoke(transport: Transport) = repository.updateTransport(transport)
}
