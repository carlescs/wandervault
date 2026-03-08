package cat.company.wandervault.ui.screens

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.usecase.CopyDocumentToInternalStorageUseCase
import cat.company.wandervault.domain.usecase.DeleteDocumentUseCase
import cat.company.wandervault.domain.usecase.DeleteFolderUseCase
import cat.company.wandervault.domain.usecase.GetDocumentsInFolderUseCase
import cat.company.wandervault.domain.usecase.GetRootDocumentsUseCase
import cat.company.wandervault.domain.usecase.GetRootFoldersUseCase
import cat.company.wandervault.domain.usecase.GetSubFoldersUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.SaveDocumentUseCase
import cat.company.wandervault.domain.usecase.SaveFolderUseCase
import cat.company.wandervault.domain.usecase.SaveTripDescriptionUseCase
import cat.company.wandervault.domain.usecase.SummarizeDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateFolderUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * ViewModel for the Trip Documents tab.
 *
 * Manages folder navigation (root → sub-folder → sub-sub-folder, etc.) and
 * exposes the current level's folders and documents as a [StateFlow].
 *
 * Write operations (create/rename/delete) catch constraint and validation failures and surface
 * them as a transient [TripDocumentsUiState.Success.writeError]. Call [clearError] to dismiss.
 *
 * @param tripId The ID of the trip whose documents are shown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripDocumentsViewModel(
    private val tripId: Int,
    private val getRootFolders: GetRootFoldersUseCase,
    private val getSubFolders: GetSubFoldersUseCase,
    private val getDocumentsInFolder: GetDocumentsInFolderUseCase,
    private val getRootDocuments: GetRootDocumentsUseCase,
    private val saveFolder: SaveFolderUseCase,
    private val updateFolder: UpdateFolderUseCase,
    private val deleteFolder: DeleteFolderUseCase,
    private val saveDocument: SaveDocumentUseCase,
    private val updateDocument: UpdateDocumentUseCase,
    private val deleteDocument: DeleteDocumentUseCase,
    private val copyDocumentToInternalStorage: CopyDocumentToInternalStorageUseCase,
    private val summarizeDocument: SummarizeDocumentUseCase,
    private val getTrip: GetTripUseCase,
    private val saveTripDescription: SaveTripDescriptionUseCase,
) : ViewModel() {

    /** Stack of folders the user has navigated into; empty = at root. */
    private val _folderStack = MutableStateFlow<List<TripDocumentFolder>>(emptyList())

    private val _uiState = MutableStateFlow<TripDocumentsUiState>(TripDocumentsUiState.Loading)
    val uiState: StateFlow<TripDocumentsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _folderStack.flatMapLatest { stack ->
                val currentFolder = stack.lastOrNull()
                val foldersFlow = if (currentFolder == null) {
                    getRootFolders(tripId)
                } else {
                    getSubFolders(currentFolder.id)
                }
                val documentsFlow = if (currentFolder == null) {
                    getRootDocuments(tripId)
                } else {
                    getDocumentsInFolder(currentFolder.id)
                }
                combine(foldersFlow, documentsFlow) { folders, documents ->
                    TripDocumentsUiState.Success(
                        folders = folders,
                        documents = documents,
                        currentFolder = currentFolder,
                        folderStack = stack,
                        // writeError is not preserved across data refreshes: a successful write
                        // triggers a new DB emission which clears any prior error naturally.
                    )
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /** Navigates into [folder], pushing it onto the folder stack. */
    fun openFolder(folder: TripDocumentFolder) {
        _folderStack.value = _folderStack.value + folder
    }

    /**
     * Navigates up one level in the folder hierarchy.
     * No-op when already at the root level.
     */
    fun navigateUp() {
        val stack = _folderStack.value
        if (stack.isNotEmpty()) {
            _folderStack.value = stack.dropLast(1)
        }
    }

    /** Returns `true` if the user can navigate up (i.e. not at the root level). */
    fun canNavigateUp(): Boolean = _folderStack.value.isNotEmpty()

    /** Dismisses the current error message (if any). */
    fun clearError() {
        val current = _uiState.value as? TripDocumentsUiState.Success ?: return
        _uiState.value = current.copy(writeError = null)
    }

    /** Creates a new folder with [name] at the current navigation level. */
    fun createFolder(name: String) {
        val parentFolderId = _folderStack.value.lastOrNull()?.id
        launchWrite {
            saveFolder(
                TripDocumentFolder(
                    tripId = tripId,
                    name = name.trim(),
                    parentFolderId = parentFolderId,
                ),
            )
        }
    }

    /** Renames [folder] to [newName]. */
    fun renameFolder(folder: TripDocumentFolder, newName: String) {
        launchWrite { updateFolder(folder.copy(name = newName.trim())) }
    }

    /** Permanently removes [folder] and all its contents. */
    fun removeFolder(folder: TripDocumentFolder) {
        launchWrite { deleteFolder(folder) }
    }

    /**
     * Copies the file at [sourceUri] to internal storage and attaches the resulting document to
     * the current folder, or to the trip root when no folder is open.
     *
     * [name] will be used as the document display name. [mimeType] should be the MIME type of
     * the file (e.g. "application/pdf"). After saving the document, ML Kit is used to extract a
     * summary and any relevant trip information. The summary is stored in the document record, and
     * any trip information found is saved as the trip's AI description if not already set.
     *
     * If the file copy fails the error is surfaced via [DocumentsWriteError.Generic].
     */
    fun addDocument(name: String, sourceUri: String, mimeType: String) {
        val currentFolder = _folderStack.value.lastOrNull()
        launchWrite {
            val internalUri = copyDocumentToInternalStorage(sourceUri)
                ?: throw IllegalStateException("Failed to copy document to internal storage")
            saveDocument(
                TripDocument(
                    tripId = tripId,
                    folderId = currentFolder?.id,
                    name = name.trim(),
                    uri = internalUri,
                    mimeType = mimeType,
                ),
            )
            extractAndApplyDocumentInfo(internalUri, mimeType, name.trim())
        }
    }

    /**
     * Runs ML Kit document extraction on the file at [internalUri], then:
     * 1. Updates the document record with the extracted summary.
     * 2. Updates the trip's AI description with any extracted trip-relevant info if the trip
     *    currently has no description.
     *
     * Extraction failures are logged and silently ignored so that the document upload always
     * succeeds even when on-device AI is unavailable.
     */
    private suspend fun extractAndApplyDocumentInfo(
        internalUri: String,
        mimeType: String,
        documentName: String,
    ) {
        try {
            val result = summarizeDocument(internalUri, mimeType) ?: return
            // Update the saved document with the extracted summary.
            // Lookup by URI is safe because internalUri is a UUID-based filename.
            val currentDocuments = if (_folderStack.value.isEmpty()) {
                getRootDocuments(tripId).first()
            } else {
                val folderId = _folderStack.value.last().id
                getDocumentsInFolder(folderId).first()
            }
            val savedDoc = currentDocuments.find { it.uri == internalUri }
            if (savedDoc != null) {
                updateDocument(savedDoc.copy(summary = result.summary))
            }
            // Update the trip description with extracted trip-relevant info if not yet set.
            val relevantInfo = result.relevantTripInfo ?: return
            val trip = getTrip(tripId).first()
            if (trip == null) {
                Log.w(TAG, "Trip $tripId not found; skipping description update for $documentName")
                return
            }
            if (trip.aiDescription == null) {
                saveTripDescription(trip, relevantInfo)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Document info extraction failed for $documentName", e)
        }
    }

    /** Renames [document] to [newName]. */
    fun renameDocument(document: TripDocument, newName: String) {
        launchWrite { updateDocument(document.copy(name = newName.trim())) }
    }

    /** Permanently removes [document]. */
    fun removeDocument(document: TripDocument) {
        launchWrite { deleteDocument(document) }
    }

    /**
     * Launches a write coroutine that catches:
     * - [IllegalArgumentException] from repository-level root-folder uniqueness checks → [DocumentsWriteError.DuplicateName]
     * - [SQLiteConstraintException] from Room unique-index violations on sub-folders or documents → [DocumentsWriteError.DuplicateName]
     * - All other [Exception] types → [DocumentsWriteError.Generic]
     *
     * Errors are surfaced as a transient [DocumentsWriteError] on the UI state rather than
     * crashing the app or cancelling the ViewModel scope.
     */
    private fun launchWrite(block: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Write operation rejected (duplicate name): ${e.message}")
                setWriteError(DocumentsWriteError.DuplicateName)
            } catch (e: SQLiteConstraintException) {
                Log.w(TAG, "Write operation rejected (constraint violation): ${e.message}")
                setWriteError(DocumentsWriteError.DuplicateName)
            } catch (e: Exception) {
                Log.e(TAG, "Write operation failed", e)
                setWriteError(DocumentsWriteError.Generic)
            }
        }
    }

    private fun setWriteError(error: DocumentsWriteError) {
        val current = _uiState.value as? TripDocumentsUiState.Success ?: return
        _uiState.value = current.copy(writeError = error)
    }

    companion object {
        private const val TAG = "TripDocumentsViewModel"
    }
}
