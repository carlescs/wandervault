package cat.company.wandervault.data.mlkit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.OrganizationPlan
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.AppPreferencesRepository
import cat.company.wandervault.domain.repository.DocumentSummaryRepository
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
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
class DocumentSummaryRepositoryImpl(
    private val context: Context,
    private val appPreferences: AppPreferencesRepository,
) : DocumentSummaryRepository {

    private val generationClient by lazy { Generation.getClient() }
    private val textRecognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun isAvailable(): Boolean = withContext(Dispatchers.IO) {
        appPreferences.getAiEnabled() && generationClient.checkStatus() != FeatureStatus.UNAVAILABLE
    }

    /**
     * Reads the document at [fileUri] and uses on-device Gemini Nano to extract a summary and
     * any trip-relevant information.
     *
     * Rather than asking the small model for everything in one response, the work is split into
     * focused calls that each get the full output budget:
     * 1. **Summary** – 2–3 sentences in the user's AI language.
     * 2. **Classification** – which booking kinds (flight / hotel / activity) the document holds.
     * 3. **Extraction** – one call per detected kind, with a type-specific prompt and a worked
     *    example. If nothing structured is found, a general trip-info call runs instead.
     *
     * The structured result is then checked against the source text by [validateExtraction].
     *
     * Returns `null` only when the on-device AI feature is permanently
     * [FeatureStatus.UNAVAILABLE] on this device, or when the document text cannot be read.
     * Any transient failure (model download error, generation error, etc.) is thrown as an
     * exception so callers can distinguish permanent unavailability from retriable errors.
     */
    override suspend fun extractDocumentInfo(
        fileUri: String,
        mimeType: String,
        tripYear: Int?,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)?,
    ): DocumentExtractionResult? {
        if (!appPreferences.getAiEnabled()) return null
        val text = readDocumentText(fileUri, mimeType) ?: return null
        return withContext(Dispatchers.IO) {
            when (generationClient.checkStatus()) {
                FeatureStatus.UNAVAILABLE -> return@withContext null
                FeatureStatus.DOWNLOADABLE -> awaitDownload(onDownloadProgress)
                FeatureStatus.AVAILABLE -> Unit
            }
            val summary = generate(buildSummaryPrompt(text), SUMMARY_MAX_TOKENS, SamplingProfile.FACTUAL)
                ?: return@withContext null
            val types = parseDocumentTypes(
                generate(buildClassificationPrompt(text), CLASSIFY_MAX_TOKENS, SamplingProfile.DETERMINISTIC)
                    .orEmpty(),
            )
            val structured = parseStructuredInfo(
                types.mapNotNull { type ->
                    generate(
                        buildExtractionPrompt(type, text, tripYear),
                        MAX_OUTPUT_TOKENS,
                        SamplingProfile.DETERMINISTIC,
                    )
                }.joinToString("\n"),
            )
            val result = if (!structured.isEmpty) {
                DocumentExtractionResult(
                    summary = summary,
                    flightInfoList = structured.flights,
                    hotelInfoList = structured.hotels,
                    activityInfoList = structured.activities,
                )
            } else {
                val generalInfo = generate(
                    buildGeneralInfoPrompt(text, tripYear),
                    GENERAL_INFO_MAX_TOKENS,
                    SamplingProfile.FACTUAL,
                )
                buildExtractionResult(summary, generalInfo)
            }
            validateExtraction(result, text, tripYear)
        }
    }

    /**
     * Sends [prompt] to Gemini Nano and returns the trimmed text of the first candidate, or `null`
     * when the model produced no (or only blank) output.
     */
    private suspend fun generate(prompt: String, maxTokens: Int, profile: SamplingProfile): String? {
        val response = generationClient.generateContent(buildGenerationRequest(prompt, maxTokens, profile))
        return response.candidates.firstOrNull()?.text?.trim()?.ifBlank { null }
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

    override suspend fun suggestDocumentName(
        fileUri: String,
        mimeType: String,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)?,
    ): String? = withContext(Dispatchers.IO) {
        if (!appPreferences.getAiEnabled()) return@withContext null
        when (generationClient.checkStatus()) {
            FeatureStatus.UNAVAILABLE -> return@withContext null
            FeatureStatus.DOWNLOADABLE -> awaitDownload(onDownloadProgress)
            FeatureStatus.AVAILABLE -> Unit
        }
        val text = readDocumentText(fileUri, mimeType) ?: return@withContext null
        val prompt = delimitedPrompt(
            tag = DOCUMENT_TAG,
            data = text,
            instructions = "Suggest a concise filename for the travel document above " +
                "(2 to 5 words, no file extension, use spaces between words), naming what it is " +
                "and, if present, the place or provider (e.g. \"Iberia flight to London\"). " +
                "Return only the filename, nothing else. " +
                "Write it in ${appPreferences.resolvedAiLanguageName()}.",
        )
        val candidateText = generate(prompt, NAME_SUGGESTION_MAX_TOKENS, SamplingProfile.DETERMINISTIC)
            ?: return@withContext null
        normalizeSuggestedFilename(candidateText)
    }

    /** Prompt for the 2–3 sentence document summary, written in the user's AI language. */
    private fun buildSummaryPrompt(documentText: String): String = delimitedPrompt(
        tag = DOCUMENT_TAG,
        data = documentText,
        instructions = "Summarize the travel document above in 2–3 sentences: what kind of " +
            "document it is, what it is for, and its key dates and places. " +
            "Only state facts that are written in the document. " +
            "Keep names, codes and booking references exactly as written. " +
            "Write the summary in ${appPreferences.resolvedAiLanguageName()}.",
    )

    /** Prompt asking which structured booking kinds the document contains. */
    private fun buildClassificationPrompt(documentText: String): String = delimitedPrompt(
        tag = DOCUMENT_TAG,
        data = documentText,
        instructions = """
            Which kinds of booking does the document above contain? Choose from:
            FLIGHT – boarding pass, e-ticket or flight itinerary
            HOTEL – hotel or accommodation booking, reservation or voucher
            ACTIVITY – tour, museum or attraction ticket, restaurant reservation or experience
            OTHER – anything else (train or bus ticket, insurance, visa, car rental, …)
            Reply only with the matching words separated by commas, for example "FLIGHT" or "FLIGHT, HOTEL".
        """.trimIndent(),
    )

    /**
     * Type-specific extraction prompt with a worked example.
     *
     * Values are extracted in the document's own language (no "respond in" instruction) so that
     * names, codes and references stay verbatim and can be checked by [validateExtraction].
     */
    private fun buildExtractionPrompt(
        type: StructuredDocumentType,
        documentText: String,
        tripYear: Int?,
    ): String {
        val spec = EXTRACTION_SPECS.getValue(type)
        val instructions = buildString {
            appendLine("Extract every ${spec.itemName} from the document above.")
            appendLine("Output one line per ${spec.itemName} in exactly this format, and nothing else:")
            appendLine(spec.format)
            appendLine("Rules:")
            appendLine("- Copy names, codes and references exactly as written in the document. Do not translate them.")
            appendLine("- Leave a field empty (nothing between the pipes) if it is not in the document. Never guess.")
            appendLine("- Write dates as YYYY-MM-DD and times as 24-hour HH:MM.")
            if (tripYear != null) {
                appendLine("- When a date has no year, use $tripYear.")
            }
            appendLine()
            appendLine("Example (from a different document):")
            appendLine("Document: ${spec.exampleDocument}")
            appendLine("Output:")
            append(spec.exampleOutput)
        }
        return delimitedPrompt(DOCUMENT_TAG, documentText, instructions)
    }

    /** Prompt for documents with no structured bookings: free-form trip-relevant facts. */
    private fun buildGeneralInfoPrompt(documentText: String, tripYear: Int?): String = delimitedPrompt(
        tag = DOCUMENT_TAG,
        data = documentText,
        instructions = buildString {
            append(
                "List the trip-relevant information in the document above (travel dates, " +
                    "destinations, booking references, coverage or validity periods) as short lines. " +
                    "Only include facts that are written in the document. " +
                    "Keep names, codes and references exactly as written. ",
            )
            if (tripYear != null) append("When a date has no year, use $tripYear. ")
            append(
                "If there is no such information, reply exactly: None. " +
                    "Write in ${appPreferences.resolvedAiLanguageName()}.",
            )
        },
    )

    override suspend fun askQuestion(
        fileUri: String,
        mimeType: String,
        question: String,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)?,
    ): String? = withContext(Dispatchers.IO) {
        if (!appPreferences.getAiEnabled()) return@withContext null
        when (generationClient.checkStatus()) {
            FeatureStatus.UNAVAILABLE -> return@withContext null
            FeatureStatus.DOWNLOADABLE -> awaitDownload(onDownloadProgress)
            FeatureStatus.AVAILABLE -> Unit
        }
        val text = readDocumentText(fileUri, mimeType) ?: return@withContext null
        val prompt = delimitedPrompt(
            tag = DOCUMENT_TAG,
            data = text,
            instructions = "Answer the question below using only the travel document above. " +
                "If the document does not contain the answer, say that it is not in the document. " +
                "Keep names, codes and references exactly as written. " +
                "Respond in ${appPreferences.resolvedAiLanguageName()}.\n\n" +
                "Question: $question",
        )
        generate(prompt, QUESTION_MAX_OUTPUT_TOKENS, SamplingProfile.FACTUAL)
    }

    override suspend fun suggestOrganization(
        documents: List<TripDocument>,
        existingFolderNames: List<String>,
        onDownloadProgress: ((bytesDownloaded: Long) -> Unit)?,
    ): OrganizationPlan? = withContext(Dispatchers.IO) {
        if (!appPreferences.getAiEnabled()) return@withContext null
        if (documents.isEmpty()) return@withContext OrganizationPlan(emptyList())
        when (generationClient.checkStatus()) {
            FeatureStatus.UNAVAILABLE -> return@withContext null
            FeatureStatus.DOWNLOADABLE -> awaitDownload(onDownloadProgress)
            FeatureStatus.AVAILABLE -> Unit
        }
        val prompt = buildOrganizationPrompt(documents, existingFolderNames)
        val rawOutput = generate(prompt, AUTO_ORGANIZE_MAX_TOKENS, SamplingProfile.DETERMINISTIC)
            ?: return@withContext OrganizationPlan(emptyList())
        parseOrganizationResponse(rawOutput, documents)
    }

    private fun buildOrganizationPrompt(
        documents: List<TripDocument>,
        existingFolderNames: List<String> = emptyList(),
    ): String {
        val documentList = buildString {
            documents.forEachIndexed { index, doc ->
                val summary = doc.summary
                if (summary != null) {
                    appendLine("${index + 1}. ${doc.name} — $summary")
                } else {
                    appendLine("${index + 1}. ${doc.name}")
                }
            }
        }
        val instructions = buildString {
            appendLine(
                "Organize the numbered travel documents above into folders. Group related " +
                    "documents together (e.g. flights, hotels, insurance, visas).",
            )
            if (existingFolderNames.isNotEmpty()) {
                appendLine(
                    "The following folders already exist. Prefer reusing these exact names over " +
                        "creating new ones with the same meaning (including synonyms or translations):",
                )
                existingFolderNames.forEach { appendLine("- $it") }
            }
            appendLine(
                "Every document must be assigned to exactly one folder. " +
                    "All numbers from 1 to ${documents.size} must appear in the output.",
            )
            appendLine(
                "Use ${appPreferences.resolvedAiLanguageName()} for folder names. " +
                    "The keywords FOLDER: and DOC: must appear exactly as shown — do not translate them.",
            )
            appendLine("Output ONLY lines in this format — no preamble, no explanation, no other text:")
            appendLine("FOLDER:<folder name>")
            appendLine("DOC:<comma-separated document numbers>")
            appendLine()
            appendLine("Example output:")
            appendLine("FOLDER:Flights")
            appendLine("DOC:1,3")
            appendLine("FOLDER:Hotels")
            append("DOC:2")
        }
        return delimitedPrompt(DOCUMENTS_TAG, documentList, instructions)
    }

    /**
     * Prompt pieces for one [StructuredDocumentType] used by [buildExtractionPrompt].
     *
     * @property itemName Human-readable name of one row (e.g. "flight leg").
     * @property format The exact line format, starting with the type's marker.
     * @property exampleDocument A short, made-up document snippet.
     * @property exampleOutput The exact output expected for [exampleDocument].
     */
    private data class ExtractionSpec(
        val itemName: String,
        val format: String,
        val exampleDocument: String,
        val exampleOutput: String,
    )

    companion object {
        private const val TAG = "DocumentSummaryRepo"

        /** Delimiter tag wrapping document text in prompts. */
        private const val DOCUMENT_TAG = "document"

        /** Delimiter tag wrapping the numbered document list in the auto-organize prompt. */
        private const val DOCUMENTS_TAG = "documents"

        /** Maximum tokens Gemini Nano may generate for the document summary. */
        private const val SUMMARY_MAX_TOKENS = 160

        /** Maximum tokens Gemini Nano may generate for the document classification. */
        private const val CLASSIFY_MAX_TOKENS = 16

        /** Maximum tokens Gemini Nano may generate for general (non-structured) trip info. */
        private const val GENERAL_INFO_MAX_TOKENS = 200

        /** Per-type extraction prompt pieces, each with a short worked example. */
        private val EXTRACTION_SPECS = mapOf(
            StructuredDocumentType.FLIGHT to ExtractionSpec(
                itemName = "flight leg",
                format = "${FLIGHT_MARKER}<airline>|<flight number>|<booking reference>|" +
                    "<departure city>|<arrival city>|<departure date>|<departure time>|<arrival time>",
                exampleDocument = "\"IBERIA BOARDING PASS · Flight IB 3166 · Barcelona → London Heathrow · " +
                    "12 MAR 2025 · Departs 7:05 PM · Arrives 8:30 PM · Booking ref XK7P2Q\"",
                exampleOutput = "${FLIGHT_MARKER}Iberia|IB 3166|XK7P2Q|Barcelona|London Heathrow|" +
                    "2025-03-12|19:05|20:30",
            ),
            StructuredDocumentType.HOTEL to ExtractionSpec(
                itemName = "hotel booking",
                format = "${HOTEL_MARKER}<hotel name>|<address>|<booking reference>|" +
                    "<check-in date>|<check-out date>",
                exampleDocument = "\"Booking confirmed! Hotel Miramar, Passeig de Colom 7, Barcelona. " +
                    "Check-in: Fri 14 March 2025. Check-out: Mon 17 March 2025. Confirmation no. 4417820935\"",
                exampleOutput = "${HOTEL_MARKER}Hotel Miramar|Passeig de Colom 7, Barcelona|4417820935|" +
                    "2025-03-14|2025-03-17",
            ),
            StructuredDocumentType.ACTIVITY to ExtractionSpec(
                itemName = "activity",
                format = "${ACTIVITY_MARKER}<title>|<short description>|<date>|<time>|<confirmation number>",
                exampleDocument = "\"Your tickets: Sagrada Família – Guided tour with tower access. " +
                    "15/03/2025, entry 10:30. 2 adults. Order SF-88231\"",
                exampleOutput = "${ACTIVITY_MARKER}Sagrada Família guided tour|Guided tour with tower access, " +
                    "2 adults|2025-03-15|10:30|SF-88231",
            ),
        )

        /** Maximum characters passed to Gemini Nano to stay within context limits. */
        private const val MAX_DOCUMENT_CHARS = 4_000

        /** Maximum tokens Gemini Nano may generate for the extraction response (hard limit: 256). */
        private const val MAX_OUTPUT_TOKENS = 256

        /** Maximum tokens Gemini Nano may generate for a filename suggestion. */
        private const val NAME_SUGGESTION_MAX_TOKENS = 32

        /** Maximum tokens Gemini Nano may generate for a free-form question answer. */
        private const val QUESTION_MAX_OUTPUT_TOKENS = 256

        /** Maximum tokens Gemini Nano may generate for the auto-organize response (hard limit: 256). */
        private const val AUTO_ORGANIZE_MAX_TOKENS = 256

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
