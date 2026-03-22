package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.OrganizationPlan
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.DocumentSummaryRepository

/**
 * Use-case that uses on-device ML Kit to suggest an automated folder organization for a list
 * of travel documents.
 */
class AutoOrganizeDocumentsUseCase(private val repository: DocumentSummaryRepository) {
    suspend fun isAvailable(): Boolean = repository.isAvailable()
    suspend operator fun invoke(
        documents: List<TripDocument>,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)? = null,
    ): OrganizationPlan? = repository.suggestOrganization(documents, onDownloadProgress)
}
