package cat.company.wandervault.domain.repository

/**
 * Extracts plain text from an image identified by a URI using on-device ML Kit Text Recognition.
 */
interface TextRecognitionRepository {

    /**
     * Runs ML Kit Text Recognition on the image at [imageUri] and returns the extracted text.
     *
     * @param imageUri A `content://` or `file://` URI pointing to the source image.
     * @return The extracted text, or `null` if extraction fails or produces no result.
     */
    suspend fun extractTextFromImage(imageUri: String): String?
}
