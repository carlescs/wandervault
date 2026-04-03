package cat.company.wandervault.data.repository

import cat.company.wandervault.BuildConfig
import cat.company.wandervault.domain.model.ImageSearchResult
import cat.company.wandervault.domain.repository.ImageSearchRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Searches for images using the Pexels REST API.
 *
 * To use this feature, provide a valid Pexels API key (obtainable for free at
 * https://www.pexels.com/api/) via the `PEXELS_API_KEY` property in `local.properties` or
 * the `PEXELS_API_KEY` environment variable. The key is surfaced through [BuildConfig] at
 * build time. Leave the property unset to disable online image search.
 */
class ImageSearchRepositoryImpl : ImageSearchRepository {

    override suspend fun searchImages(query: String): Result<List<ImageSearchResult>> =
        withContext(Dispatchers.IO) {
            if (BuildConfig.PEXELS_API_KEY.isBlank()) return@withContext Result.success(emptyList())
            try {
                val encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.name())
                val urlString =
                    "https://api.pexels.com/v1/search?query=$encodedQuery&per_page=40"
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("Authorization", BuildConfig.PEXELS_API_KEY)
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

    private fun parseResults(json: String): List<ImageSearchResult> {
        val root = JSONObject(json)
        val photos = root.getJSONArray("photos")
        return buildList {
            for (i in 0 until photos.length()) {
                val photo = photos.getJSONObject(i)
                val src = photo.optJSONObject("src") ?: continue
                val thumbnail = src.optString("medium").takeIf { it.isNotBlank() }
                    ?: src.optString("small").takeIf { it.isNotBlank() }
                    ?: continue
                val full = src.optString("large2x").takeIf { it.isNotBlank() }
                    ?: src.optString("large").takeIf { it.isNotBlank() }
                    ?: src.optString("original").takeIf { it.isNotBlank() }
                    ?: continue
                val description = photo.optString("alt")
                add(ImageSearchResult(thumbnailUrl = thumbnail, fullUrl = full, description = description))
            }
        }
    }

    companion object {
        private const val TIMEOUT_MS = 10_000
    }
}
