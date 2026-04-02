package cat.company.wandervault.domain.model

/**
 * Represents a document attached to a trip, optionally inside a [TripDocumentFolder].
 *
 * @param id Unique database ID (0 means not yet persisted).
 * @param tripId The ID of the trip this document belongs to.
 * @param folderId The ID of the folder this document belongs to, or `null` for root-level documents.
 * @param name Document display name, unique within its folder (or within the trip root).
 * @param uri URI pointing to the file in internal storage.
 * @param mimeType MIME type or simple type tag (e.g. "application/pdf", "image/png").
 * @param summary AI-generated summary of the document content, or `null` if not yet extracted.
 * @param remoteUrl Firebase Storage download URL for the document, or `null` if not yet uploaded.
 */
data class TripDocument(
    val id: Int = 0,
    val tripId: Int,
    val folderId: Int? = null,
    val name: String,
    val uri: String,
    val mimeType: String,
    val summary: String? = null,
    val remoteUrl: String? = null,
)
