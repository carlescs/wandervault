package cat.company.wandervault.domain.model

/**
 * Represents a single result from an online image search.
 *
 * @property thumbnailUrl URL of the small preview image suitable for display in a grid.
 * @property fullUrl URL of the full-resolution image to download when selected.
 * @property description Human-readable description or tags associated with the image.
 */
data class ImageSearchResult(
    val thumbnailUrl: String,
    val fullUrl: String,
    val description: String,
)
