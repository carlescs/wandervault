package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.repository.TransportRepository

/** Use-case that persists a new [Transport] to the repository. */
class SaveTransportUseCase(private val repository: TransportRepository) {
    suspend operator fun invoke(transport: Transport) = repository.saveTransport(transport)
}
