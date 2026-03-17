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
import cat.company.wandervault.domain.usecase.GetAllFoldersForTripUseCase
import cat.company.wandervault.domain.usecase.GetDocumentsInFolderUseCase
import cat.company.wandervault.domain.usecase.GetRootDocumentsUseCase
import cat.company.wandervault.domain.usecase.GetRootFoldersUseCase
import cat.company.wandervault.domain.usecase.GetSubFoldersUseCase
import cat.company.wandervault.domain.usecase.GetTripUseCase
import cat.company.wandervault.domain.usecase.SaveDocumentUseCase
import cat.company.wandervault.domain.usecase.SaveFolderUseCase
import cat.company.wandervault.domain.usecase.SuggestDocumentNameUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateFolderUseCase
import cat.company.wandervault.domain.usecase.UploadDocumentToDriveUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
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
    private val getAllFoldersForTrip: GetAllFoldersForTripUseCase,
    private val suggestDocumentName: SuggestDocumentNameUseCase,
    private val getTrip: GetTripUseCase,
    private val uploadDocumentToDrive: UploadDocumentToDriveUseCase,
) : ViewModel() {

    /** Stack of folders the user has navigated into; empty = at root. */
    private val _folderStack = MutableStateFlow<List<TripDocumentFolder>>(emptyList())

    /**
     * IDs of documents currently selected in multi-select mode.
     * Kept separate from the DB-driven flow so that navigation events (folder changes) can
     * clear the selection explicitly without a full state rebuild.
     */
    private val _selectedDocumentIds = MutableStateFlow<Set<Int>>(emptySet())

    private val _uiState = MutableStateFlow<TripDocumentsUiState>(TripDocumentsUiState.Loading)
    val uiState: StateFlow<TripDocumentsUiState> = _uiState.asStateFlow()

    /**
     * Current state of an in-flight filename suggestion request, or `null` when no suggestion
     * is active. Collect this in the UI to update name-input dialogs while AI analysis runs.
     */
    private val _suggestNameState = MutableStateFlow<SuggestNameUiState?>(null)
    val suggestNameState: StateFlow<SuggestNameUiState?> = _suggestNameState.asStateFlow()

    /** Tracks the running suggestion job so it can be cancelled on demand. */
    private var suggestNameJob: Job? = null

    /**
     * Tracks on-device AI availability.
     * Initialised to `false` (fail-closed) and updated once in [init] after checking the model.
     */
    private val _isAiAvailable = MutableStateFlow(false)

    init {
        // Check AI availability upfront so the suggest-name button is hidden proactively
        // on devices that do not support Gemini Nano.
        viewModelScope.launch {
            val available = try {
                suggestDocumentName.isAvailable()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "AI availability check failed; assuming unavailable", e)
                false
            }
            _isAiAvailable.value = available
        }

        viewModelScope.launch {
            // allFoldersFlow is independent of navigation level, so it is hoisted outside
            // flatMapLatest to avoid creating a new Room observer on every folder navigation.
            val allFoldersFlow = getAllFoldersForTrip(tripId)
            val contentFlow: Flow<TripDocumentsUiState.Success> =
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
                    combine(foldersFlow, documentsFlow, allFoldersFlow) {
                            folders, documents, allFolders ->
                        TripDocumentsUiState.Success(
                            folders = folders,
                            documents = documents,
                            currentFolder = currentFolder,
                            folderStack = stack,
                            allFolders = allFolders,
                            // writeError is not preserved across data refreshes: a successful write
                            // triggers a new DB emission which clears any prior error naturally.
                        )
                    }
                }
            // Merge selection and AI availability state separately to preserve them across
            // DB-driven updates.
            combine(contentFlow, _selectedDocumentIds, _isAiAvailable) { state, selectedIds, aiAvailable ->
                state.copy(selectedDocumentIds = selectedIds, isAiAvailable = aiAvailable)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /** Navigates into [folder], pushing it onto the folder stack. */
    fun openFolder(folder: TripDocumentFolder) {
        _selectedDocumentIds.value = emptySet()
        _folderStack.value = _folderStack.value + folder
    }

    /**
     * Navigates up one level in the folder hierarchy.
     * No-op when already at the root level.
     */
    fun navigateUp() {
        val stack = _folderStack.value
        if (stack.isNotEmpty()) {
            _selectedDocumentIds.value = emptySet()
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

    /**
     * Starts an ML Kit analysis on the document at [fileUri] to produce a suggested filename.
     *
     * Progress is exposed via [suggestNameState]:
     * - [SuggestNameUiState.Loading] while the request is running.
     * - [SuggestNameUiState.Downloading] while the Gemini Nano model is being downloaded.
     * - [SuggestNameUiState.Success] when a name is produced.
     * - [SuggestNameUiState.Unavailable] if the on-device AI is permanently unavailable.
     * - [SuggestNameUiState.Error] for transient failures.
     *
     * Any previous in-flight suggestion is cancelled before a new one starts.
     *
     * @param excludeName When renaming an existing document, pass its current name here so it is
     *   not counted as a conflict during uniqueness deduplication. Pass `null` when adding a new
     *   document (no name to exclude).
     */
    fun requestSuggestName(fileUri: String, mimeType: String, excludeName: String? = null) {
        suggestNameJob?.cancel()
        suggestNameJob = viewModelScope.launch {
            _suggestNameState.value = SuggestNameUiState.Loading
            try {
                val suggested = suggestDocumentName(
                    fileUri = fileUri,
                    mimeType = mimeType,
                    onDownloadProgress = { bytes ->
                        _suggestNameState.value = SuggestNameUiState.Downloading(bytes)
                    },
                )
                _suggestNameState.value = if (suggested != null) {
                    val existingNames = (_uiState.value as? TripDocumentsUiState.Success)
                        ?.documents
                        ?.map { it.name }
                        ?.toSet()
                        ?.let { names -> if (excludeName != null) names - excludeName else names }
                        ?: emptySet()
                    SuggestNameUiState.Success(makeUniqueDocumentName(suggested, existingNames))
                } else {
                    SuggestNameUiState.Unavailable
                }
            } catch (e: Exception) {
                Log.w(TAG, "Filename suggestion failed", e)
                _suggestNameState.value = SuggestNameUiState.Error(e.message)
            }
        }
    }

    /** Clears the current filename suggestion state and cancels any in-flight suggestion. */
    fun clearSuggestName() {
        suggestNameJob?.cancel()
        suggestNameJob = null
        _suggestNameState.value = null
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
     * the file (e.g. "application/pdf").
     *
     * If the file copy fails the error is surfaced via [DocumentsWriteError.Generic].
     */
    fun addDocument(name: String, sourceUri: String, mimeType: String) {
        val currentFolder = _folderStack.value.lastOrNull()
        // Capture the current folder stack value so the remote path mirrors the complete
        // on-device folder hierarchy at the time of the save, even if the user navigates
        // away before the async upload starts.
        val folderStack = _folderStack.value
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
            // Best-effort Drive upload: does not block or fail the local save.
            uploadToDriveAsync(
                localUri = internalUri,
                mimeType = mimeType,
                fileName = name.trim(),
                folderStack = folderStack,
            )
        }
    }

    /**
     * Uploads [localUri] to Drive in a separate coroutine so that a failed upload never
     * rolls back the local document save.  Logs failures for diagnostics but does not
     * surface them as a [DocumentsWriteError].
     *
     * [folderStack] is the complete list of folders the user navigated into, which is used
     * to build a remote path that mirrors the full on-device folder hierarchy.
     */
    private fun uploadToDriveAsync(
        localUri: String,
        mimeType: String,
        fileName: String,
        folderStack: List<TripDocumentFolder>,
    ) {
        viewModelScope.launch {
            try {
                // Room Flows emit their first value synchronously from the SQLite cache, so
                // firstOrNull() returns the current trip state rather than waiting for a
                // network/background update. The nullable fallback is a belt-and-suspenders
                // guard for the edge case where the trip row no longer exists.
                val tripName = getTrip(tripId).firstOrNull()?.title ?: "Trip $tripId"
                // Build the full remote path: [tripName, folder1, folder2, …] so the Drive
                // hierarchy mirrors the complete on-device folder structure, not just the leaf.
                val remotePath = buildList {
                    add(tripName)
                    addAll(folderStack.map { it.name })
                }
                uploadDocumentToDrive(
                    localUri = localUri,
                    mimeType = mimeType,
                    fileName = fileName,
                    remotePath = remotePath,
                ).onFailure { e ->
                    Log.w(TAG, "Drive upload skipped or failed for '$fileName' (tripId=$tripId)", e)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Drive upload error for '$fileName' (tripId=$tripId)", e)
            }
        }
    }

    /** Renames [document] to [newName]. */
    fun renameDocument(document: TripDocument, newName: String) {
        launchWrite { updateDocument(document.copy(name = newName.trim())) }
    }

    /** Moves [document] to the folder with [targetFolderId], or to root level when `null`. */
    fun moveDocument(document: TripDocument, targetFolderId: Int?) {
        launchWrite { updateDocument(document.copy(folderId = targetFolderId)) }
    }

    /** Permanently removes [document]. */
    fun removeDocument(document: TripDocument) {
        launchWrite { deleteDocument(document) }
    }

    // ── Multi-select operations ───────────────────────────────────────────────

    /**
     * Toggles the selection state of [document].
     * If the document is not yet selected it is added to the selection (entering selection mode
     * if this is the first selected document). If it is already selected it is removed.
     */
    fun toggleDocumentSelection(document: TripDocument) {
        val current = _selectedDocumentIds.value
        _selectedDocumentIds.value = if (document.id in current) {
            current - document.id
        } else {
            current + document.id
        }
    }

    /**
     * Selects all documents visible in the current folder.
     * No-op when no documents are present.
     */
    fun selectAllDocuments() {
        val current = _uiState.value as? TripDocumentsUiState.Success ?: return
        _selectedDocumentIds.value = current.documents.map { it.id }.toSet()
    }

    /** Clears the current selection, exiting multi-select mode. */
    fun clearSelection() {
        _selectedDocumentIds.value = emptySet()
    }

    /**
     * Permanently deletes all currently selected documents.
     * The selection is cleared immediately before the write operations begin.
     */
    fun deleteSelectedDocuments() {
        val current = _uiState.value as? TripDocumentsUiState.Success ?: return
        val selectedIds = _selectedDocumentIds.value
        if (selectedIds.isEmpty()) return
        val documentsToDelete = current.documents.filter { it.id in selectedIds }
        _selectedDocumentIds.value = emptySet()
        launchWrite {
            documentsToDelete.forEach { deleteDocument(it) }
        }
    }

    /**
     * Moves all currently selected documents to [targetFolderId], or to the trip root when `null`.
     * The selection is cleared immediately before the write operations begin.
     *
     * Each document is moved independently; a failure for one item (e.g. a name conflict in the
     * target folder) is logged and recorded, but the remaining documents are still processed.
     * If any moves fail, a [DocumentsWriteError.Generic] error is surfaced after the loop.
     */
    fun moveSelectedDocuments(targetFolderId: Int?) {
        val current = _uiState.value as? TripDocumentsUiState.Success ?: return
        val selectedIds = _selectedDocumentIds.value
        if (selectedIds.isEmpty()) return
        val documentsToMove = current.documents.filter { it.id in selectedIds }
        _selectedDocumentIds.value = emptySet()
        viewModelScope.launch {
            val failedMoves = mutableListOf<TripDocument>()
            documentsToMove.forEach { document ->
                try {
                    updateDocument(document.copy(folderId = targetFolderId))
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Failed to move document ${document.id} ('${document.name}') to folder $targetFolderId",
                        e,
                    )
                    failedMoves.add(document)
                }
            }
            if (failedMoves.isNotEmpty()) {
                setWriteError(DocumentsWriteError.Generic)
            }
        }
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

/**
 * Returns [candidate] if it is not present in [existingNames], or appends an incrementing
 * counter suffix (" 2", " 3", …) until a unique name is found.
 *
 * For example, if "Paris Flight" is taken, it returns "Paris Flight 2"; if that is also taken,
 * it returns "Paris Flight 3", and so on.
 */
internal fun makeUniqueDocumentName(candidate: String, existingNames: Set<String>): String {
    if (candidate !in existingNames) return candidate
    var counter = 2
    while ("$candidate $counter" in existingNames) counter++
    return "$candidate $counter"
}
