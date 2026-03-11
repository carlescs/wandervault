package cat.company.wandervault.domain.repository

import cat.company.wandervault.domain.model.DocumentExtractionResult

/**
 * Repository that uses ML Kit to extract information from travel documents.
 */
interface DocumentSummaryRepository {

    /**
     * Reads the document at [fileUri] and uses on-device AI to extract a summary and any
     * trip-relevant information (dates, destinations, booking references, etc.).
     *
     * @param fileUri URI of the document file in internal storage.
     * @param mimeType MIME type of the document (e.g. "text/plain", "application/pdf").
     * @param tripYear The calendar year of the trip the document belongs to, used to resolve
     *   dates that appear in the document without an explicit year component. `null` means no
     *   year hint is provided and the AI will infer the year from context.
     * @param onDownloadProgress Optional callback invoked with the number of bytes downloaded
     *   so far while the Gemini Nano model is being downloaded to the device. Not invoked when
     *   the model is already available.
     * @return [DocumentExtractionResult] with a summary and optional trip info, or `null` if
     *   on-device AI is permanently unavailable on this device or the document content cannot
     *   be read.
     * @throws Exception for transient failures (e.g. model download failure, generation error)
     *   so callers can distinguish permanent unavailability (null) from retriable errors.
     */
    suspend fun extractDocumentInfo(
        fileUri: String,
        mimeType: String,
        tripYear: Int? = null,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)? = null,
    ): DocumentExtractionResult?

    /**
     * Reads the document at [fileUri] and uses on-device AI to suggest a concise filename
     * that reflects the document's content.
     *
     * @param fileUri URI of the document file (internal storage or content URI).
     * @param mimeType MIME type of the document (e.g. "text/plain", "application/pdf").
     * @param onDownloadProgress Optional callback invoked with the number of bytes downloaded
     *   so far while the Gemini Nano model is being downloaded to the device.
     * @return A suggested filename string (without file extension), or `null` if on-device AI
     *   is permanently unavailable on this device or the document content cannot be read.
     * @throws Exception for transient failures so callers can distinguish permanent
     *   unavailability (null) from retriable errors.
     */
    suspend fun suggestDocumentName(
        fileUri: String,
        mimeType: String,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)? = null,
    ): String?
}
