package cat.company.wandervault.domain.model

/**
 * Represents a folder inside a trip's Documents section.
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param tripId The ID of the trip this folder belongs to.
 * @param name Folder display name, unique within its parent scope.
 * @param parentFolderId `null` for root-level folders; ID of the parent for sub-folders.
 */
data class TripDocumentFolder(
    val id: Int = 0,
    val tripId: Int,
    val name: String,
    val parentFolderId: Int? = null,
)
