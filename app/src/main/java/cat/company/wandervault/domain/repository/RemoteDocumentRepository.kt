package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.TripDocument

/**
 * Repository that handles uploading and downloading [TripDocument] files to/from Firebase Storage.
 *
 * Files are stored at `trips/{shareId}/documents/{documentId}`.
 */
interface RemoteDocumentRepository {
    /**
     * Uploads the local file identified by [document] to Firebase Storage under the given
     * [shareId].  Updates `TripDocument.remoteUrl` in the local Room database.
     *
     * @return The Firebase Storage download URL.
     */
    suspend fun uploadDocument(shareId: String, document: TripDocument): String

    /**
     * Downloads the remote document at [remoteUrl] and saves it to internal storage, returning
     * the local URI string.
     */
    suspend fun downloadDocument(remoteUrl: String, destinationName: String): String

    /**
     * Deletes the remote file for [document] from Firebase Storage.  No-op if [document] has no
     * [TripDocument.remoteUrl].
     */
    suspend fun deleteRemoteDocument(document: TripDocument)
}
