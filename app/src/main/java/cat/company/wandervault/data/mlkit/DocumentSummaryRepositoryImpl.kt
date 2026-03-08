package cat.company.wandervault.data.mlkit

import android.content.Context
import android.net.Uri
import android.util.Log
import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.repository.DocumentSummaryRepository
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * ML Kit implementation of [DocumentSummaryRepository] that uses the on-device Gemini Nano
 * Prompt API to summarize travel documents and extract relevant trip information.
 *
 * Text extraction is supported for `text/*` MIME types. Documents of other types (e.g. PDF,
 * images) are silently skipped and `null` is returned — a future enhancement could add
 * OCR-based text recognition for images and PDF rendering for PDF files.
 */
class DocumentSummaryRepositoryImpl(private val context: Context) : DocumentSummaryRepository {

    private val client by lazy { Generation.getClient() }

    override suspend fun extractDocumentInfo(
        fileUri: String,
        mimeType: String,
    ): DocumentExtractionResult? {
        val text = readDocumentText(fileUri, mimeType) ?: return null
        return withContext(Dispatchers.IO) {
            when (client.checkStatus()) {
                FeatureStatus.UNAVAILABLE -> return@withContext null
                FeatureStatus.DOWNLOADABLE -> awaitDownload()
                FeatureStatus.AVAILABLE -> Unit
            }
            try {
                val request = generateContentRequest(TextPart(buildPrompt(text))) {
                    maxOutputTokens = MAX_OUTPUT_TOKENS
                }
                val response = client.generateContent(request)
                val rawOutput = response.candidates.firstOrNull()?.text
                    ?.trim() ?: return@withContext null
                parseResponse(rawOutput)
            } catch (e: Exception) {
                Log.w(TAG, "Gemini Nano document extraction failed", e)
                null
            }
        }
    }

    /**
     * Waits for the Gemini Nano model to finish downloading, mirroring the pattern used in
     * [TripDescriptionRepositoryImpl].
     */
    private suspend fun awaitDownload() {
        val terminal = client.download()
            .first { it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed }
        if (terminal is DownloadStatus.DownloadFailed) throw terminal.e
    }

    /**
     * Attempts to read the document at [fileUri] as a plain-text string.
     *
     * Only `text/*` MIME types are supported; other types return `null`.
     * The content is capped at [MAX_DOCUMENT_CHARS] to stay within model limits.
     * Both `file://` and `content://` URIs are supported.
     */
    private suspend fun readDocumentText(fileUri: String, mimeType: String): String? {
        if (!mimeType.startsWith("text/")) return null
        return withContext(Dispatchers.IO) {
            try {
                val uri = Uri.parse(fileUri)
                val inputStream = if (uri.scheme == "file") {
                    val path = uri.path ?: return@withContext null
                    File(path).inputStream()
                } else {
                    context.contentResolver.openInputStream(uri) ?: return@withContext null
                }
                inputStream.use { it.reader().readText().take(MAX_DOCUMENT_CHARS) }
            } catch (e: IOException) {
                Log.w(TAG, "Failed to read document text from $fileUri", e)
                null
            }
        }
    }

    private fun buildPrompt(documentText: String): String = buildString {
        appendLine(
            "Analyze the following travel document and respond with exactly two sections " +
                "separated by the marker \"---\":",
        )
        appendLine(
            "Section 1: A brief summary (2–3 sentences) of what this document contains.",
        )
        appendLine(
            "Section 2: Any trip-relevant information found in the document " +
                "(such as travel dates, destinations, airline/hotel names, or booking reference " +
                "numbers). If nothing trip-relevant is found, write \"None\".",
        )
        appendLine()
        appendLine("Document text:")
        append(documentText)
    }

    /**
     * Parses the two-section Gemini Nano response.
     *
     * Expected format:
     * ```
     * <summary text>
     * ---
     * <trip info text or "None">
     * ```
     */
    private fun parseResponse(raw: String): DocumentExtractionResult {
        val parts = raw.split(SECTION_SEPARATOR, limit = 2)
        val summary = parts.getOrNull(0)?.trim()?.ifBlank { null } ?: raw.trim()
        val tripInfoRaw = parts.getOrNull(1)?.trim()
        val relevantTripInfo = if (tripInfoRaw.isNullOrBlank() || tripInfoRaw.equals("None", ignoreCase = true)) {
            null
        } else {
            tripInfoRaw
        }
        return DocumentExtractionResult(
            summary = summary,
            relevantTripInfo = relevantTripInfo,
        )
    }

    companion object {
        private const val TAG = "DocumentSummaryRepo"
        private const val SECTION_SEPARATOR = "---"

        /** Maximum characters read from the document to avoid exceeding model context limits. */
        private const val MAX_DOCUMENT_CHARS = 4_000

        /** Maximum tokens Gemini Nano may generate for the extraction response. */
        private const val MAX_OUTPUT_TOKENS = 300
    }
}
