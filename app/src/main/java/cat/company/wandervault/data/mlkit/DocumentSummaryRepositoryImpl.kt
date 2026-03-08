package cat.company.wandervault.data.mlkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.repository.DocumentSummaryRepository
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.google.mlkit.genai.prompt.TextPart
import com.google.mlkit.genai.prompt.generateContentRequest
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit implementation of [DocumentSummaryRepository] that uses the on-device Gemini Nano
 * Prompt API to summarize travel documents and extract relevant trip information.
 *
 * Supported MIME types:
 * - MIME types starting with `text/` — read directly as plain text.
 * - `application/pdf` — each page is rendered to a [Bitmap] using [PdfRenderer] and then
 *   passed through ML Kit Text Recognition (OCR) to produce extractable text.
 * - MIME types starting with `image/` — loaded as a [Bitmap] and passed through ML Kit
 *   Text Recognition (OCR) to extract any text visible in the image.
 *
 * Documents of unsupported types return `null`.
 */
class DocumentSummaryRepositoryImpl(private val context: Context) : DocumentSummaryRepository {

    private val generationClient by lazy { Generation.getClient() }
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    /**
     * Reads the document at [fileUri] and uses on-device Gemini Nano to extract a summary and
     * any trip-relevant information.
     *
     * Returns `null` only when the on-device AI feature is permanently
     * [FeatureStatus.UNAVAILABLE] on this device, or when the document text cannot be read.
     * Any transient failure (model download error, generation error, etc.) is thrown as an
     * exception so callers can distinguish permanent unavailability from retriable errors.
     */
    override suspend fun extractDocumentInfo(
        fileUri: String,
        mimeType: String,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)?,
    ): DocumentExtractionResult? {
        val text = readDocumentText(fileUri, mimeType) ?: return null
        return withContext(Dispatchers.IO) {
            when (generationClient.checkStatus()) {
                FeatureStatus.UNAVAILABLE -> return@withContext null
                FeatureStatus.DOWNLOADABLE -> awaitDownload(onDownloadProgress)
                FeatureStatus.AVAILABLE -> Unit
            }
            val request = generateContentRequest(TextPart(buildPrompt(text))) {
                maxOutputTokens = MAX_OUTPUT_TOKENS
            }
            val response = generationClient.generateContent(request)
            val rawOutput = response.candidates.firstOrNull()?.text
                ?.trim() ?: return@withContext null
            parseResponse(rawOutput)
        }
    }

    /**
     * Waits for the Gemini Nano model to finish downloading.
     *
     * Uses [onEach] to forward [DownloadStatus.DownloadProgress] events to [onProgress] while
     * the flow is still running, then [first] to suspend until a terminal state is reached.
     */
    private suspend fun awaitDownload(onProgress: ((Long) -> Unit)?) {
        val terminal = generationClient.download()
            .onEach { status ->
                if (status is DownloadStatus.DownloadProgress) {
                    onProgress?.invoke(status.totalBytesDownloaded)
                }
            }
            .first { it is DownloadStatus.DownloadCompleted || it is DownloadStatus.DownloadFailed }
        if (terminal is DownloadStatus.DownloadFailed) throw terminal.e
    }

    /**
     * Dispatches text extraction to the appropriate reader based on [mimeType].
     * Returns `null` if the MIME type is unsupported or extraction fails.
     */
    private suspend fun readDocumentText(fileUri: String, mimeType: String): String? = when {
        mimeType.startsWith("text/") -> readPlainText(fileUri)
        mimeType.startsWith("application/pdf") -> readPdfText(fileUri)
        mimeType.startsWith("image/") -> readImageText(fileUri)
        else -> null
    }

    /**
     * Opens an [InputStream] for [uri], supporting both `file://` and `content://` schemes.
     * Returns `null` if [uri] has no path (for `file://` URIs) or if the content resolver returns
     * no stream. May throw [java.io.FileNotFoundException] or [SecurityException] — callers are
     * responsible for catching these exceptions.
     */
    private fun openInputStream(uri: Uri): InputStream? = if (uri.scheme == "file") {
        val path = uri.path ?: return null
        File(path).inputStream()
    } else {
        context.contentResolver.openInputStream(uri)
    }

    /**
     * Reads [fileUri] as a plain-text string.
     * The content is capped at [MAX_DOCUMENT_CHARS] to stay within model limits.
     * Both `file://` and `content://` URIs are supported.
     */
    private suspend fun readPlainText(fileUri: String): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = openInputStream(Uri.parse(fileUri)) ?: return@withContext null
            inputStream.use { it.reader().readText().take(MAX_DOCUMENT_CHARS) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to read document text from $fileUri", e)
            null
        }
    }

    /**
     * Extracts text from an image at [fileUri] using ML Kit Text Recognition (OCR).
     * The image is decoded to a [Bitmap] and passed through [recognizeBitmapText].
     * Returns `null` if the image cannot be decoded or no text is found.
     * Both `file://` and `content://` URIs are supported.
     */
    private suspend fun readImageText(fileUri: String): String? {
        val bitmap = withContext(Dispatchers.IO) {
            try {
                val inputStream = openInputStream(Uri.parse(fileUri)) ?: return@withContext null
                inputStream.use { stream ->
                    BitmapFactory.decodeStream(stream) ?: run {
                        Log.w(TAG, "BitmapFactory returned null for $fileUri (unsupported or corrupt image)")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to decode image from $fileUri", e)
                null
            }
        } ?: return null
        // recognizeBitmapText is a suspend function; the bitmap is guaranteed to be
        // safe to recycle once it returns (whether successfully or with an exception).
        return try {
            recognizeBitmapText(bitmap).ifBlank { null }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to recognize text in image $fileUri", e)
            null
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Extracts text from a PDF at [fileUri] by:
     * 1. Rendering each page to a [Bitmap] via [PdfRenderer] (Android built-in, API 21+).
     * 2. Running ML Kit Text Recognition (OCR) on each bitmap.
     * 3. Concatenating the recognized text from all pages.
     *
     * Processing is limited to the first [MAX_PDF_PAGES] pages and the combined text is capped
     * at [MAX_DOCUMENT_CHARS] characters to stay within Gemini Nano's context window.
     *
     * Returns `null` if the file cannot be opened, no text is found, or an error occurs.
     * Both `file://` and `content://` URIs are supported.
     */
    private suspend fun readPdfText(fileUri: String): String? = withContext(Dispatchers.IO) {
        val uri = Uri.parse(fileUri)
        val pfd = try {
            if (uri.scheme == "file") {
                val path = uri.path ?: return@withContext null
                ParcelFileDescriptor.open(File(path), ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r") ?: return@withContext null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open PDF descriptor for $fileUri", e)
            return@withContext null
        }
        try {
            val allText = StringBuilder()
            PdfRenderer(pfd).use { renderer ->
                val pageCount = minOf(renderer.pageCount, MAX_PDF_PAGES)
                for (i in 0 until pageCount) {
                    if (allText.length >= MAX_DOCUMENT_CHARS) break
                    renderer.openPage(i).use { page ->
                        // Render at 2× the native PDF point size for better OCR quality.
                        val bitmap = Bitmap.createBitmap(
                            page.width * RENDER_SCALE,
                            page.height * RENDER_SCALE,
                            Bitmap.Config.ARGB_8888,
                        )
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        try {
                            val pageText = recognizeBitmapText(bitmap)
                            if (pageText.isNotBlank()) {
                                if (allText.isNotEmpty()) allText.append('\n')
                                allText.append(pageText)
                            }
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
            }
            allText.toString().take(MAX_DOCUMENT_CHARS).ifBlank { null }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to extract text from PDF $fileUri", e)
            null
        } finally {
            pfd.close()
        }
    }

    /**
     * Runs ML Kit Text Recognition on [bitmap] and returns the recognized text.
     * Wraps the Task-based ML Kit API in a coroutine.
     */
    private suspend fun recognizeBitmapText(bitmap: Bitmap): String =
        suspendCancellableCoroutine { cont ->
            textRecognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { visionText -> cont.resume(visionText.text) }
                .addOnFailureListener { e -> cont.resumeWithException(e) }
        }

    private fun buildPrompt(documentText: String): String = buildString {
        appendLine(
            "Analyze the following travel document and respond with exactly two sections " +
                "separated by the marker \"---\":",
        )
        appendLine(
            "Section 1: A brief summary (2–3 sentences) of what this document contains.",
        )
        appendLine("Section 2: Identify the document type and extract structured info:")
        appendLine(
            "- If it is a FLIGHT document (boarding pass, e-ticket, flight itinerary), " +
                "output exactly one line: " +
                "${FLIGHT_MARKER}<airline>|<flight number>|<booking reference>|<departure city>|<arrival city>",
        )
        appendLine(
            "- If it is a HOTEL document (booking confirmation, reservation, hotel voucher), " +
                "output exactly one line: ${HOTEL_MARKER}<hotel name>|<address>|<booking reference>",
        )
        appendLine(
            "- Otherwise, list any trip-relevant info (travel dates, destinations, " +
                "booking references). If none, write \"None\".",
        )
        appendLine(
            "Leave a field blank (empty between pipes) if the information is not found.",
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
     * FLIGHT|<airline>|<flight number>|<booking ref>|<departure>|<arrival>
     * ```
     * or
     * ```
     * <summary text>
     * ---
     * HOTEL|<hotel name>|<address>|<booking ref>
     * ```
     * or
     * ```
     * <summary text>
     * ---
     * <general trip info text or "None">
     * ```
     */
    private fun parseResponse(raw: String): DocumentExtractionResult {
        val parts = raw.split(SECTION_SEPARATOR, limit = 2)
        val summary = parts.getOrNull(0)?.trim()?.ifBlank { null } ?: raw.trim()
        val infoRaw = parts.getOrNull(1)?.trim()
        if (infoRaw.isNullOrBlank() || infoRaw.equals("None", ignoreCase = true)) {
            return DocumentExtractionResult(summary = summary)
        }
        // Use only the first non-blank line of section 2 to check for structured markers.
        val infoFirstLine = infoRaw.lines().firstOrNull { it.isNotBlank() }?.trim()
        if (infoFirstLine.isNullOrBlank()) {
            return DocumentExtractionResult(summary = summary)
        }
        if (infoFirstLine.startsWith(FLIGHT_MARKER, ignoreCase = true)) {
            val fields = infoFirstLine.substring(FLIGHT_MARKER.length).split("|")
            return DocumentExtractionResult(
                summary = summary,
                flightInfo = FlightInfo(
                    airline = fields.getOrNull(0)?.trim()?.ifBlank { null },
                    flightNumber = fields.getOrNull(1)?.trim()?.ifBlank { null },
                    bookingReference = fields.getOrNull(2)?.trim()?.ifBlank { null },
                    departurePlace = fields.getOrNull(3)?.trim()?.ifBlank { null },
                    arrivalPlace = fields.getOrNull(4)?.trim()?.ifBlank { null },
                ),
            )
        }
        if (infoFirstLine.startsWith(HOTEL_MARKER, ignoreCase = true)) {
            val fields = infoFirstLine.substring(HOTEL_MARKER.length).split("|")
            return DocumentExtractionResult(
                summary = summary,
                hotelInfo = HotelInfo(
                    name = fields.getOrNull(0)?.trim()?.ifBlank { null },
                    address = fields.getOrNull(1)?.trim()?.ifBlank { null },
                    bookingReference = fields.getOrNull(2)?.trim()?.ifBlank { null },
                ),
            )
        }
        // Fallback: treat the entire section 2 as general trip-relevant info (legacy behaviour).
        return DocumentExtractionResult(summary = summary, relevantTripInfo = infoRaw)
    }

    companion object {
        private const val TAG = "DocumentSummaryRepo"
        private const val SECTION_SEPARATOR = "---"
        private const val FLIGHT_MARKER = "FLIGHT|"
        private const val HOTEL_MARKER = "HOTEL|"

        /** Maximum characters passed to Gemini Nano to stay within context limits. */
        private const val MAX_DOCUMENT_CHARS = 4_000

        /** Maximum tokens Gemini Nano may generate for the extraction response (hard limit: 256). */
        private const val MAX_OUTPUT_TOKENS = 256

        /** Maximum number of PDF pages to process (avoids excessive OCR time on large documents). */
        private const val MAX_PDF_PAGES = 10

        /**
         * Scale factor applied when rendering PDF pages to bitmaps.
         * A value of 2 renders at 144 DPI (2× the 72 DPI PDF point size), which provides
         * good OCR accuracy without excessive memory usage.
         */
        private const val RENDER_SCALE = 2
    }
}
