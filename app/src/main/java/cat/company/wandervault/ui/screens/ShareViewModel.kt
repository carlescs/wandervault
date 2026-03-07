package cat.company.wandervault.ui.screens

import android.util.Log
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.repository.TextRecognitionRepository
import cat.company.wandervault.domain.usecase.GetTripsUseCase
import cat.company.wandervault.domain.usecase.SaveTripDocumentUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.LocalDateTime
import java.util.UUID

/**
 * ViewModel for the Share / "Save to Trip" screen.
 *
 * Receives an [Intent] with ACTION_SEND shared content, copies the file to internal storage,
 * optionally extracts text via ML Kit, and saves the resulting [TripDocument] once the user
 * confirms.
 */
class ShareViewModel(
    private val context: Context,
    private val getTripsUseCase: GetTripsUseCase,
    private val saveTripDocumentUseCase: SaveTripDocumentUseCase,
    private val textRecognitionRepository: TextRecognitionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShareUiState>(ShareUiState.Processing)
    val uiState: StateFlow<ShareUiState> = _uiState.asStateFlow()

    /** Cached local URI of the copied file so we can clean it up if the user cancels. */
    private var pendingLocalUri: String? = null

    /**
     * Processes the incoming [intent] (ACTION_SEND).
     * Copies the shared file / text into internal storage and extracts text where applicable.
     */
    fun handleIntent(intent: Intent) {
        if (_uiState.value !is ShareUiState.Processing) return
        viewModelScope.launch {
            try {
                val trips = getTripsUseCase().first()
                val mimeType = intent.type ?: "application/octet-stream"
                when {
                    mimeType.startsWith("text/") -> handleTextIntent(intent, mimeType, trips)
                    mimeType.startsWith("image/") -> handleImageIntent(intent, mimeType, trips)
                    else -> handleFileIntent(intent, mimeType, trips)
                }
            } catch (e: Exception) {
                _uiState.value = ShareUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun handleTextIntent(intent: Intent, mimeType: String, trips: List<Trip>) {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT) ?: "Shared text"
        val fileName = "$subject.txt"
        val localUri = saveTextToFile(text, fileName)
        pendingLocalUri = localUri
        _uiState.value = ShareUiState.Ready(
            fileName = subject,
            mimeType = mimeType,
            localUri = localUri,
            extractedText = text,
            trips = trips,
            selectedTripId = trips.firstOrNull()?.id,
            folder = "",
        )
    }

    private suspend fun handleImageIntent(intent: Intent, mimeType: String, trips: List<Trip>) {
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: run {
            _uiState.value = ShareUiState.Error("No image URI in share intent")
            return
        }
        val localUri = copyUriToDocuments(uri, mimeType)
        pendingLocalUri = localUri
        val extractedText = if (localUri != null) {
            runCatching { textRecognitionRepository.extractTextFromImage(localUri) }.getOrNull()
        } else {
            null
        }
        val name = getDisplayName(uri) ?: "Shared image"
        _uiState.value = ShareUiState.Ready(
            fileName = name,
            mimeType = mimeType,
            localUri = localUri,
            extractedText = extractedText,
            trips = trips,
            selectedTripId = trips.firstOrNull()?.id,
            folder = "Images",
        )
    }

    private suspend fun handleFileIntent(intent: Intent, mimeType: String, trips: List<Trip>) {
        @Suppress("DEPRECATION")
        val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM) ?: run {
            _uiState.value = ShareUiState.Error("No file URI in share intent")
            return
        }
        val localUri = copyUriToDocuments(uri, mimeType)
        pendingLocalUri = localUri
        val name = getDisplayName(uri) ?: "Shared file"
        val defaultFolder = when {
            mimeType == "application/pdf" -> "PDFs"
            else -> "Files"
        }
        _uiState.value = ShareUiState.Ready(
            fileName = name,
            mimeType = mimeType,
            localUri = localUri,
            extractedText = null,
            trips = trips,
            selectedTripId = trips.firstOrNull()?.id,
            folder = defaultFolder,
        )
    }

    /** Updates the selected trip. */
    fun selectTrip(tripId: Int) {
        val current = _uiState.value as? ShareUiState.Ready ?: return
        _uiState.value = current.copy(selectedTripId = tripId)
    }

    /** Updates the folder name. */
    fun setFolder(folder: String) {
        val current = _uiState.value as? ShareUiState.Ready ?: return
        _uiState.value = current.copy(folder = folder)
    }

    /**
     * Saves the document to the selected trip and transitions to [ShareUiState.Saved].
     */
    fun saveDocument() {
        val current = _uiState.value as? ShareUiState.Ready ?: return
        val tripId = current.selectedTripId ?: return
        val localUri = current.localUri ?: run {
            _uiState.value = ShareUiState.Error("No file to save")
            return
        }
        viewModelScope.launch {
            try {
                saveTripDocumentUseCase(
                    TripDocument(
                        tripId = tripId,
                        name = current.fileName,
                        localUri = localUri,
                        mimeType = current.mimeType,
                        folder = current.folder.ifBlank { null },
                        extractedText = current.extractedText,
                        createdAt = LocalDateTime.now(),
                    ),
                )
                pendingLocalUri = null
                _uiState.value = ShareUiState.Saved
            } catch (e: Exception) {
                _uiState.value = ShareUiState.Error(e.message ?: "Failed to save document")
            }
        }
    }

    /** Cleans up any pending copied file if the user cancels. */
    fun cancel() {
        val uri = pendingLocalUri ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val path = Uri.parse(uri).path ?: return@launch
                File(path).delete()
            } catch (_: IOException) { /* ignore */ }
        }
        pendingLocalUri = null
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private suspend fun copyUriToDocuments(uri: Uri, mimeType: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "bin"
                val dir = File(context.filesDir, "documents").also { it.mkdirs() }
                val file = File(dir, "${UUID.randomUUID()}.$extension")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                Uri.fromFile(file).toString()
            } catch (_: Exception) {
                null
            }
        }

    private suspend fun saveTextToFile(text: String, fileName: String): String? =
        withContext(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "documents").also { it.mkdirs() }
                val file = File(dir, "${UUID.randomUUID()}_$fileName")
                file.writeText(text)
                Uri.fromFile(file).toString()
            } catch (_: Exception) {
                null
            }
        }

    private fun getDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve display name for URI: $uri", e)
            null
        }
    }

    companion object {
        private const val TAG = "ShareViewModel"
    }
}
