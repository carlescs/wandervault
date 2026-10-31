package cat.company.wandervault.domain.repository

/**
 * Repository that recognises text in images on-device (OCR).
 */
interface TextRecognitionRepository {

    /**
     * Recognises the text visible in the image at [imageUri] (a `content://` or `file://` URI).
     *
     * The image is processed entirely on-device and is not stored by this repository.
     *
     * @return The recognised text, or `null` when no text was found.
     * @throws Exception when the image cannot be read or recognition fails.
     */
    suspend fun recognizeText(imageUri: String): String?
}
