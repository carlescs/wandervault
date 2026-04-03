package cat.company.wandervault.data.repository

import android.content.Context
import android.net.Uri
import cat.company.wandervault.domain.model.ImageSearchResult
import cat.company.wandervault.domain.repository.ImageSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Searches for images using the Pixabay REST API and saves downloaded results to internal storage.
 *
 * To use this feature, replace [PIXABAY_API_KEY] with a valid key obtained for free at
 * https://pixabay.com/api/docs/.
 *
 * For production builds, store the key in `local.properties` and expose it via a BuildConfig
 * field rather than committing it to source control.
 */
class ImageSearchRepositoryImpl(private val context: Context) : ImageSearchRepository {

    override suspend fun searchImages(query: String): Result<List<ImageSearchResult>> =
        withContext(Dispatchers.IO) {
            if (PIXABAY_API_KEY.isBlank()) return@withContext Result.success(emptyList())
            try {
                val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                val urlString =
                    "https://pixabay.com/api/?key=$PIXABAY_API_KEY" +
                        "&q=$encodedQuery&image_type=photo&per_page=40&safesearch=true"
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                val body = try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return@withContext Result.failure(
                            IOException("HTTP ${connection.responseCode}"),
                        )
                    }
                    connection.inputStream.use { inputStream ->
                        inputStream.bufferedReader().use { reader ->
                            reader.readText()
                        }
                    }
                } finally {
                    connection.disconnect()
                }
                Result.success(parseResults(body))
            } catch (e: IOException) {
                Result.failure(e)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun downloadImage(url: String): String? = withContext(Dispatchers.IO) {
        val imagesDir = File(context.filesDir, IMAGES_DIR_NAME)
        if (!imagesDir.exists() && !imagesDir.mkdirs()) return@withContext null
        val extension = extractExtension(url)
        val file = File(imagesDir, "${UUID.randomUUID()}.$extension")
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            } finally {
                connection.disconnect()
            }
            Uri.fromFile(file).toString()
        } catch (e: IOException) {
            file.delete()
            null
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    private fun extractExtension(url: String): String = imageUrlExtension(url)

    private fun parseResults(json: String): List<ImageSearchResult> {
        val root = JSONObject(json)
        val hits = root.getJSONArray("hits")
        return buildList {
            for (i in 0 until hits.length()) {
                val hit = hits.getJSONObject(i)
                val thumbnail = hit.optString("previewURL").takeIf { it.isNotBlank() }
                    ?: continue
                val full = hit.optString("largeImageURL").takeIf { it.isNotBlank() }
                    ?: hit.optString("webformatURL").takeIf { it.isNotBlank() }
                    ?: continue
                val tags = hit.optString("tags")
                add(ImageSearchResult(thumbnailUrl = thumbnail, fullUrl = full, description = tags))
            }
        }
    }

    companion object {
        /**
         * Replace with your own free Pixabay API key from https://pixabay.com/api/docs/.
         * Store the actual key in `local.properties` and surface it via BuildConfig to avoid
         * committing secrets to source control. Leave blank to disable online image search.
         */
        const val PIXABAY_API_KEY: String = ""

        private const val IMAGES_DIR_NAME = "images"
        private const val TIMEOUT_MS = 10_000
    }
}

/**
 * Extracts the file extension from an image [url], stripping any query parameters first.
 *
 * Returns `"jpg"` as a fallback when no valid extension can be determined.
 */
internal fun imageUrlExtension(url: String): String {
    // Strip query string before extracting the extension to handle URLs like
    // "https://example.com/image.jpg?size=large".
    val path = url.substringBefore('?').substringAfterLast('/')
    val ext = path.substringAfterLast('.', "").take(4).filter { it.isLetterOrDigit() }
    return ext.ifBlank { "jpg" }
}
