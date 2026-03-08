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
     * @return [DocumentExtractionResult] with a summary and optional trip info,
     *         or `null` if on-device AI is unavailable or the document cannot be processed.
     */
    suspend fun extractDocumentInfo(fileUri: String, mimeType: String): DocumentExtractionResult?
}
