package cat.company.wandervault.ui.screens

import android.database.sqlite.SQLiteConstraintException
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.model.OrganizationPlan
import cat.company.wandervault.domain.usecase.CopyDocumentToInternalStorageUseCase
import cat.company.wandervault.domain.usecase.AutoOrganizeDocumentsUseCase
import cat.company.wandervault.domain.usecase.DeleteDocumentUseCase
import cat.company.wandervault.domain.usecase.DeleteFolderUseCase
import cat.company.wandervault.domain.usecase.GetAllDocumentsForTripUseCase
import cat.company.wandervault.domain.usecase.GetAllFoldersForTripUseCase
import cat.company.wandervault.domain.usecase.GetDocumentsInFolderUseCase
import cat.company.wandervault.domain.usecase.GetRootDocumentsUseCase
import cat.company.wandervault.domain.usecase.GetRootFoldersUseCase
import cat.company.wandervault.domain.usecase.GetSubFoldersUseCase
import cat.company.wandervault.domain.usecase.SaveDocumentUseCase
import cat.company.wandervault.domain.usecase.SaveFolderUseCase
import cat.company.wandervault.domain.usecase.SuggestDocumentNameUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateFolderUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
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
    private val getAllFoldersForTrip: GetAllFoldersForTripUseCase,
    private val getAllDocumentsForTrip: GetAllDocumentsForTripUseCase,
    private val suggestDocumentName: SuggestDocumentNameUseCase,
    private val autoOrganizeDocuments: AutoOrganizeDocumentsUseCase,
) : ViewModel() {

    /** Stack of folders the user has navigated into; empty = at root. */
    private val _folderStack = MutableStateFlow<List<TripDocumentFolder>>(emptyList())

    /**
     * IDs of documents currently selected in multi-select mode.
     * Kept separate from the DB-driven flow so that navigation events (folder changes) can
     * clear the selection explicitly without a full state rebuild.
     */
    private val _selectedDocumentIds = MutableStateFlow<Set<Int>>(emptySet())

    /**
     * IDs of folders currently selected in multi-select mode.
     * Kept separate from the DB-driven flow so that navigation events (folder changes) can
     * clear the selection explicitly without a full state rebuild.
     */
    private val _selectedFolderIds = MutableStateFlow<Set<Int>>(emptySet())

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

    /** Tracks the running auto-organize job so it can be cancelled on demand. */
    private var autoOrganizeJob: Job? = null

    /**
     * Current state of an in-flight or completed auto-organize request, or `null` when no
     * auto-organize is active.
     */
    private val _autoOrganizeState = MutableStateFlow<AutoOrganizeUiState?>(null)

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
            // Trip-wide flows have a single Room observer each for the lifetime of the ViewModel;
            // they are combined with the navigation-scoped content flow outside flatMapLatest so
            // that folder navigation never creates new subscriptions for them.
            val allFoldersFlow = getAllFoldersForTrip(tripId)
            val allDocumentsFlow = getAllDocumentsForTrip(tripId)

            // Navigation-scoped flow: only the folders and documents for the current level.
            // allFolders/allDocuments are intentionally excluded here and merged below.
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
                }
            // Merge trip-wide flows, selection, AI availability, and auto-organize state outside
            // flatMapLatest so each has exactly one Room/StateFlow observer for the ViewModel's
            // lifetime, regardless of folder navigation.
            combine(
                contentFlow,
                allFoldersFlow,
                allDocumentsFlow,
                _selectedDocumentIds,
                _isAiAvailable,
            ) { state, allFolders, allDocuments, selectedIds, aiAvailable ->
                state.copy(
                    allFolders = allFolders,
                    allDocuments = allDocuments,
                    selectedDocumentIds = selectedIds,
                    isAiAvailable = aiAvailable,
                )
            }.combine(_selectedFolderIds) { state, selectedFolderIds ->
                state.copy(selectedFolderIds = selectedFolderIds)
            }.combine(_autoOrganizeState) { state, autoOrganize ->
                state.copy(autoOrganizeState = autoOrganize)
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    /** Navigates into [folder], pushing it onto the folder stack. */
    fun openFolder(folder: TripDocumentFolder) {
        _selectedDocumentIds.value = emptySet()
        _selectedFolderIds.value = emptySet()
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
            _selectedFolderIds.value = emptySet()
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

    // ── Auto-organize operations ──────────────────────────────────────────────

    /**
     * Starts an AI analysis to produce a suggested folder organization plan.
     *
     * When at the trip root, **all** documents (including those already in folders) are analyzed
     * so the AI has the full picture and can avoid duplicate folder names that are synonyms or
     * translations of existing ones. When inside a subfolder, only the documents in that folder
     * are analyzed to avoid inadvertently reorganizing the rest of the trip.
     *
     * Folders are created (or reused) at the current navigation level only.
     *
     * Progress is exposed via [TripDocumentsUiState.Success.autoOrganizeState]:
     * - [AutoOrganizeUiState.Loading] while the request is running.
     * - [AutoOrganizeUiState.Downloading] while the Gemini Nano model is being downloaded.
     * - [AutoOrganizeUiState.ReadyToConfirm] when a plan is produced (user must confirm).
     * - [AutoOrganizeUiState.Unavailable] if the on-device AI is permanently unavailable.
     * - [AutoOrganizeUiState.Error] for transient failures.
     *
     * Any previous in-flight request is cancelled before a new one starts.
     */
    fun requestAutoOrganize() {
        autoOrganizeJob?.cancel()
        autoOrganizeJob = viewModelScope.launch {
            _autoOrganizeState.value = AutoOrganizeUiState.Loading
            try {
                val currentFolder = _folderStack.value.lastOrNull()
                val parentFolderId = currentFolder?.id
                // At root: analyze all trip documents (including those already in folders) to
                // give the AI complete context and prevent synonym/translation folder duplicates.
                // Inside a subfolder: scope to that folder's documents only to avoid unexpectedly
                // reorganizing documents that belong to other parts of the trip.
                val documents = if (currentFolder == null) {
                    getAllDocumentsForTrip(tripId).first()
                } else {
                    getDocumentsInFolder(currentFolder.id).first()
                }
                if (documents.isEmpty()) {
                    _autoOrganizeState.value = null
                    return@launch
                }
                val existingFolderNames = getAllFoldersForTrip(tripId)
                    .first()
                    .filter { it.parentFolderId == parentFolderId }
                    .map { it.name }
                val plan = autoOrganizeDocuments(
                    documents = documents,
                    existingFolderNames = existingFolderNames,
                    onDownloadProgress = { bytes ->
                        _autoOrganizeState.value = AutoOrganizeUiState.Downloading(bytes)
                    },
                )
                _autoOrganizeState.value = if (plan != null) {
                    AutoOrganizeUiState.ReadyToConfirm(plan)
                } else {
                    AutoOrganizeUiState.Unavailable
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Auto-organize failed", e)
                _autoOrganizeState.value = AutoOrganizeUiState.Error(e.message)
            }
        }
    }

    /** Cancels any in-flight auto-organize request and clears the state. */
    fun cancelAutoOrganize() {
        autoOrganizeJob?.cancel()
        autoOrganizeJob = null
        _autoOrganizeState.value = null
    }

    /**
     * Applies the given [plan] by creating the suggested folders (or reusing existing ones
     * when the AI's suggested name matches an existing folder name case-insensitively) and
     * moving documents into them.
     *
     * Reusing existing folders prevents duplicate folders that are synonyms or translations
     * of each other (e.g. "Flights" vs "Vuelos") from being created.
     *
     * The auto-organize state is cleared immediately so the dialog closes while the background
     * work runs. Any failures are surfaced via [DocumentsWriteError.Generic].
     */
    fun applyOrganization(plan: OrganizationPlan) {
        _autoOrganizeState.value = null
        val parentFolderId = _folderStack.value.lastOrNull()?.id
        viewModelScope.launch {
            // Snapshot existing folders under the current parent; track created names as we go
            // so that new AI-suggested names get unique suffixes when needed (e.g. "Flights 2").
            val allFolders = getAllFoldersForTrip(tripId).first()
            val existingFolders = allFolders.filter { it.parentFolderId == parentFolderId }
            val occupiedNames = existingFolders.map { it.name }.toMutableSet()
            val failedItems = mutableListOf<String>()
            assignments@ for (assignment in plan.folderAssignments) {
                try {
                    val suggestedName = assignment.folderName.trim()
                    // Reuse an existing folder whose name matches case-insensitively to avoid
                    // creating duplicate folders with synonymous or translated names.
                    val matchedFolder = existingFolders.firstOrNull {
                        it.name.trim().equals(suggestedName, ignoreCase = true)
                    }
                    val targetFolder = if (matchedFolder != null) {
                        matchedFolder
                    } else {
                        val folderName = makeUniqueFolderName(suggestedName, occupiedNames)
                        occupiedNames.add(folderName)
                        saveFolder(
                            TripDocumentFolder(
                                tripId = tripId,
                                name = folderName,
                                parentFolderId = parentFolderId,
                            ),
                        )
                        // Wait for Room to emit the updated folder list that contains the new folder,
                        // then retrieve the generated ID.
                        val newFolder = getAllFoldersForTrip(tripId)
                            .first { folders ->
                                folders.any { it.name == folderName && it.parentFolderId == parentFolderId }
                            }
                            .firstOrNull { it.name == folderName && it.parentFolderId == parentFolderId }
                        if (newFolder == null) {
                            Log.e(TAG, "Could not find newly created folder '$folderName'")
                            failedItems.add(assignment.folderName)
                            continue@assignments
                        }
                        newFolder
                    }
                    for (document in assignment.documents) {
                        try {
                            updateDocument(document.copy(folderId = targetFolder.id))
                        } catch (e: Exception) {
                            Log.e(
                                TAG,
                                "Failed to move document ${document.id} to folder ${targetFolder.id}",
                                e,
                            )
                            failedItems.add(document.name)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to create folder '${assignment.folderName}'", e)
                    failedItems.add(assignment.folderName)
                }
            }
            if (failedItems.isNotEmpty()) {
                setWriteError(DocumentsWriteError.Generic)
            }
        }
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
     * Toggles the selection state of [folder].
     * If the folder is not yet selected it is added to the selection (entering selection mode
     * if this is the first selected item). If it is already selected it is removed.
     */
    fun toggleFolderSelection(folder: TripDocumentFolder) {
        val current = _selectedFolderIds.value
        _selectedFolderIds.value = if (folder.id in current) {
            current - folder.id
        } else {
            current + folder.id
        }
    }

    /**
     * Selects all folders and documents visible in the current folder.
     * No-op when no items are present.
     */
    fun selectAll() {
        val current = _uiState.value as? TripDocumentsUiState.Success ?: return
        _selectedDocumentIds.value = current.documents.map { it.id }.toSet()
        _selectedFolderIds.value = current.folders.map { it.id }.toSet()
    }

    /** Clears the current selection, exiting multi-select mode. */
    fun clearSelection() {
        _selectedDocumentIds.value = emptySet()
        _selectedFolderIds.value = emptySet()
    }

    /**
     * Permanently deletes all currently selected documents and folders.
     * The selection is cleared immediately before the write operations begin.
     */
    fun deleteSelectedDocuments() {
        val current = _uiState.value as? TripDocumentsUiState.Success ?: return
        val selectedDocIds = _selectedDocumentIds.value
        val selectedFolderIds = _selectedFolderIds.value
        if (selectedDocIds.isEmpty() && selectedFolderIds.isEmpty()) return
        val documentsToDelete = current.documents.filter { it.id in selectedDocIds }
        val foldersToDelete = current.folders.filter { it.id in selectedFolderIds }
        _selectedDocumentIds.value = emptySet()
        _selectedFolderIds.value = emptySet()
        launchWrite {
            documentsToDelete.forEach { deleteDocument(it) }
            foldersToDelete.forEach { deleteFolder(it) }
        }
    }

    /**
     * Moves all currently selected documents and folders to [targetFolderId], or to the trip root
     * when `null`. The selection is cleared immediately before the write operations begin.
     *
     * Each item is moved independently; a failure for one item (e.g. a name conflict in the
     * target folder) is logged and recorded, but the remaining items are still processed.
     * If any moves fail, a [DocumentsWriteError.Generic] error is surfaced after the loop.
     */
    fun moveSelectedItems(targetFolderId: Int?) {
        val current = _uiState.value as? TripDocumentsUiState.Success ?: return
        val selectedDocIds = _selectedDocumentIds.value
        val selectedFolderIds = _selectedFolderIds.value
        if (selectedDocIds.isEmpty() && selectedFolderIds.isEmpty()) return
        val documentsToMove = current.documents.filter { it.id in selectedDocIds }
        val foldersToMove = current.folders.filter { it.id in selectedFolderIds }
        _selectedDocumentIds.value = emptySet()
        _selectedFolderIds.value = emptySet()
        viewModelScope.launch {
            var anyFailed = false
            documentsToMove.forEach { document ->
                try {
                    updateDocument(document.copy(folderId = targetFolderId))
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Failed to move document ${document.id} ('${document.name}') to folder $targetFolderId",
                        e,
                    )
                    anyFailed = true
                }
            }
            foldersToMove.forEach { folder ->
                try {
                    updateFolder(folder.copy(parentFolderId = targetFolderId))
                } catch (e: Exception) {
                    Log.e(
                        TAG,
                        "Failed to move folder ${folder.id} ('${folder.name}') to folder $targetFolderId",
                        e,
                    )
                    anyFailed = true
                }
            }
            if (anyFailed) {
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

/**
 * Returns [candidate] if it is not present in [existingNames], or appends an incrementing
 * counter suffix (" 2", " 3", …) until a unique name is found.
 *
 * Used when creating folders during auto-organization to avoid duplicate folder names.
 */
internal fun makeUniqueFolderName(candidate: String, existingNames: Set<String>): String {
    if (candidate !in existingNames) return candidate
    var counter = 2
    while ("$candidate $counter" in existingNames) counter++
    return "$candidate $counter"
}
