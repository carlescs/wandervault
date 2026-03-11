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
import cat.company.wandervault.domain.usecase.SaveDocumentUseCase
import cat.company.wandervault.domain.usecase.SaveFolderUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateFolderUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    init {
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
            // Merge selection state separately to preserve it across DB-driven updates.
            combine(contentFlow, _selectedDocumentIds) { state, selectedIds ->
                state.copy(selectedDocumentIds = selectedIds)
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
