package cat.company.wandervault.domain.model

import java.time.LocalDateTime

/**
 * Represents a document or file shared into the app and attached to a trip.
 *
 * @param id Auto-generated primary key.
 * @param tripId ID of the trip this document belongs to.
 * @param name Display name of the document.
 * @param localUri Internal `file://` URI where the file is stored.
 * @param mimeType MIME type of the file (e.g. `application/pdf`, `text/plain`).
 * @param folder Optional folder name used to group documents in the Documents tab.
 * @param extractedText Text extracted from the document via ML Kit (if any).
 * @param createdAt Timestamp when the document was saved.
 */
data class TripDocument(
    val id: Int = 0,
    val tripId: Int,
    val name: String,
    val localUri: String,
    val mimeType: String,
    val folder: String? = null,
    val extractedText: String? = null,
    val createdAt: LocalDateTime = LocalDateTime.now(),
)
