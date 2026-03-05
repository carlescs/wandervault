package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.repository.TransportRepository

/** Use-case that removes a [Transport] from the repository. */
class DeleteTransportUseCase(private val repository: TransportRepository) {
    suspend operator fun invoke(transport: Transport) = repository.deleteTransport(transport)
}
