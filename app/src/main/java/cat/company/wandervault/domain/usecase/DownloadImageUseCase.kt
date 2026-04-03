package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.ImageSearchRepository

/** Downloads the image at [url] to internal storage and returns its `file://` URI. */
class DownloadImageUseCase(private val repository: ImageSearchRepository) {
    suspend operator fun invoke(url: String): String? = repository.downloadImage(url)
}
