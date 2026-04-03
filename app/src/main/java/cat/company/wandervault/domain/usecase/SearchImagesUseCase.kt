package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.ImageSearchResult
import cat.company.wandervault.domain.repository.ImageSearchRepository

/** Searches for images online matching the provided [query]. */
class SearchImagesUseCase(private val repository: ImageSearchRepository) {
    suspend operator fun invoke(query: String): List<ImageSearchResult> =
        repository.searchImages(query)
}
