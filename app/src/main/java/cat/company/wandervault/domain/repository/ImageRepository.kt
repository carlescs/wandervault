package cat.company.wandervault.domain.repository

/**
 * Handles storage of trip images in a location that persists across app updates.
 */
interface ImageRepository {
    /**
     * Copies the image at [sourceUri] to the app's internal storage.
     *
     * @param sourceUri A `content://` or `file://` URI pointing to the source image.
     * @return The URI of the copied file, or `null` if copying fails.
     */
    suspend fun copyToInternalStorage(sourceUri: String): String?

    /**
     * Deletes the internal-storage image file identified by [fileUri].
     * Does nothing if [fileUri] does not point to a file inside the app's internal storage.
     */
    suspend fun deleteFromInternalStorage(fileUri: String)
}
