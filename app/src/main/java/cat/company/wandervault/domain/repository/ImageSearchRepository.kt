package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.ImageSearchResult

/**
 * Searches for images online and downloads selected results to internal storage.
 */
interface ImageSearchRepository {
    /**
     * Searches for images matching [query] and returns a list of results.
     *
     * @param query The search term (e.g. "Paris Eiffel Tower").
     * @return A list of matching [ImageSearchResult] items, or an empty list on error.
     */
    suspend fun searchImages(query: String): List<ImageSearchResult>

    /**
     * Downloads the image at [url] and saves it to the app's internal storage.
     *
     * @param url A publicly accessible image URL.
     * @return The `file://` URI of the saved image, or `null` if the download fails.
     */
    suspend fun downloadImage(url: String): String?
}
