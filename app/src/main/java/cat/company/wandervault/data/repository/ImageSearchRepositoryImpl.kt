package cat.company.wandervault.data.repository

import android.content.Context
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
import java.util.UUID

/**
 * Searches for images using the Pixabay REST API and saves downloaded results to internal storage.
 *
 * To use this feature, replace [PIXABAY_API_KEY] with a valid key obtained for free at
 * https://pixabay.com/api/docs/. Searches will return an empty list when the key is absent.
 */
class ImageSearchRepositoryImpl(private val context: Context) : ImageSearchRepository {

    override suspend fun searchImages(query: String): List<ImageSearchResult> =
        withContext(Dispatchers.IO) {
            if (PIXABAY_API_KEY.isBlank()) return@withContext emptyList()
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val urlString =
                    "https://pixabay.com/api/?key=$PIXABAY_API_KEY" +
                        "&q=$encodedQuery&image_type=photo&per_page=40&safesearch=true"
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                val body = try {
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        return@withContext emptyList()
                    }
                    connection.inputStream.bufferedReader().readText()
                } finally {
                    connection.disconnect()
                }
                parseResults(body)
            } catch (e: IOException) {
                emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    override suspend fun downloadImage(url: String): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            val bytes = try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                connection.inputStream.use { it.readBytes() }
            } finally {
                connection.disconnect()
            }
            val imagesDir = File(context.filesDir, "images")
            if (!imagesDir.exists() && !imagesDir.mkdirs()) return@withContext null
            val extension = url.substringAfterLast('.', "jpg").take(4).filter { it.isLetterOrDigit() }
            val file = File(imagesDir, "${UUID.randomUUID()}.$extension")
            file.writeBytes(bytes)
            android.net.Uri.fromFile(file).toString()
        } catch (e: IOException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun parseResults(json: String): List<ImageSearchResult> {
        return try {
            val root = JSONObject(json)
            val hits = root.getJSONArray("hits")
            buildList {
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
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        /**
         * Replace with your own free Pixabay API key from https://pixabay.com/api/docs/.
         * Leave blank to disable online image search.
         */
        const val PIXABAY_API_KEY: String = ""

        private const val TIMEOUT_MS = 10_000
    }
}
