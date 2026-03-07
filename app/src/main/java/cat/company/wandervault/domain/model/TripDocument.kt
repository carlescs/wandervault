package cat.company.wandervault.domain.model

/**
 * Represents a document stored inside a [TripDocumentFolder].
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param folderId The ID of the folder this document belongs to.
 * @param name Document display name, unique within its folder.
 * @param uri URI pointing to the file in internal storage.
 * @param mimeType MIME type or simple type tag (e.g. "application/pdf", "image/png").
 */
data class TripDocument(
    val id: Int = 0,
    val folderId: Int,
    val name: String,
    val uri: String,
    val mimeType: String,
)
