package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.ImageSearchResult

/**
 * Searches for images online.
 */
interface ImageSearchRepository {
    /**
     * Searches for images matching [query].
     *
     * @param query The search term (e.g. "Paris Eiffel Tower").
     * @return [Result.success] with a (possibly empty) list of results on success,
     *   or [Result.failure] when a network or API error occurs.
     */
    suspend fun searchImages(query: String): Result<List<ImageSearchResult>>
}
