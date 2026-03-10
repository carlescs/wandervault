package cat.company.wandervault.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.domain.usecase.GetAllFoldersForTripUseCase
import cat.company.wandervault.domain.usecase.GetDocumentByIdUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ViewModel for the Document Info screen.
 *
 * Loads the [DocumentInfoUiState] for a single document identified by [documentId], including
 * the document model, the file size read from internal storage, and the containing folder name.
 *
 * @param documentId The ID of the document to display.
 * @param getDocumentById Use-case that streams the document entity.
 * @param getAllFoldersForTrip Use-case that streams all folders in the document's trip.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DocumentInfoViewModel(
    private val documentId: Int,
    private val getDocumentById: GetDocumentByIdUseCase,
    private val getAllFoldersForTrip: GetAllFoldersForTripUseCase,
) : ViewModel() {

    val uiState: StateFlow<DocumentInfoUiState> = getDocumentById(documentId)
        .flatMapLatest { document ->
            if (document == null) {
                flowOf(DocumentInfoUiState.NotFound)
            } else {
                flow {
                    val fileSize = resolveFileSize(document.uri)
                    emitAll(
                        getAllFoldersForTrip(document.tripId).map { folders ->
                            val folderName = document.folderId?.let { fid ->
                                folders.find { it.id == fid }?.name
                            }
                            DocumentInfoUiState.Success(
                                document = document,
                                fileSizeBytes = fileSize,
                                folderName = folderName,
                            )
                        },
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DocumentInfoUiState.Loading,
        )

    private suspend fun resolveFileSize(uri: String): Long? = withContext(Dispatchers.IO) {
        try {
            val path = Uri.parse(uri).path ?: return@withContext null
            val file = File(path)
            if (file.exists()) file.length() else null
        } catch (_: Exception) {
            null
        }
    }
}
