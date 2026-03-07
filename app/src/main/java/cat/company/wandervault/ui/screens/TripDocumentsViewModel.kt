package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.domain.usecase.DeleteDocumentUseCase
import cat.company.wandervault.domain.usecase.DeleteFolderUseCase
import cat.company.wandervault.domain.usecase.GetDocumentsInFolderUseCase
import cat.company.wandervault.domain.usecase.GetRootFoldersUseCase
import cat.company.wandervault.domain.usecase.GetSubFoldersUseCase
import cat.company.wandervault.domain.usecase.SaveDocumentUseCase
import cat.company.wandervault.domain.usecase.SaveFolderUseCase
import cat.company.wandervault.domain.usecase.UpdateDocumentUseCase
import cat.company.wandervault.domain.usecase.UpdateFolderUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

/**
 * ViewModel for the Trip Documents tab.
 *
 * Manages folder navigation (root → sub-folder → sub-sub-folder, etc.) and
 * exposes the current level's folders and documents as a [StateFlow].
 *
 * @param tripId The ID of the trip whose documents are shown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TripDocumentsViewModel(
    private val tripId: Int,
    private val getRootFolders: GetRootFoldersUseCase,
    private val getSubFolders: GetSubFoldersUseCase,
    private val getDocumentsInFolder: GetDocumentsInFolderUseCase,
    private val saveFolder: SaveFolderUseCase,
    private val updateFolder: UpdateFolderUseCase,
    private val deleteFolder: DeleteFolderUseCase,
    private val saveDocument: SaveDocumentUseCase,
    private val updateDocument: UpdateDocumentUseCase,
    private val deleteDocument: DeleteDocumentUseCase,
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
                    flowOf(emptyList())
                } else {
                    getDocumentsInFolder(currentFolder.id)
                }
                combine(foldersFlow, documentsFlow) { folders, documents ->
                    TripDocumentsUiState.Success(
                        folders = folders,
                        documents = documents,
                        currentFolder = currentFolder,
                        folderStack = stack,
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

    /** Creates a new folder with [name] at the current navigation level. */
    fun createFolder(name: String) {
        val parentFolderId = _folderStack.value.lastOrNull()?.id
        viewModelScope.launch {
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
        viewModelScope.launch {
            updateFolder(folder.copy(name = newName.trim()))
        }
    }

    /** Permanently removes [folder] and all its contents. */
    fun removeFolder(folder: TripDocumentFolder) {
        viewModelScope.launch {
            deleteFolder(folder)
        }
    }

    /**
     * Attaches a document to the currently open folder.
     *
     * Documents can only be added to a folder, not at the root level. This method is a no-op
     * when no folder is currently open (i.e. the user is at the root level).
     * The UI should only expose this action when the user has navigated into a folder.
     */
    fun addDocument(name: String, uri: String, mimeType: String) {
        val currentFolder = _folderStack.value.lastOrNull() ?: return
        viewModelScope.launch {
            saveDocument(
                TripDocument(
                    folderId = currentFolder.id,
                    name = name.trim(),
                    uri = uri,
                    mimeType = mimeType,
                ),
            )
        }
    }

    /** Renames [document] to [newName]. */
    fun renameDocument(document: TripDocument, newName: String) {
        viewModelScope.launch {
            updateDocument(document.copy(name = newName.trim()))
        }
    }

    /** Permanently removes [document]. */
    fun removeDocument(document: TripDocument) {
        viewModelScope.launch {
            deleteDocument(document)
        }
    }
}
