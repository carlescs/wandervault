package cat.company.wandervault.ui.screens

import android.webkit.MimeTypeMap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.OrganizationPlan
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Documents tab entry point for the Trip Detail screen.
 *
 * @param tripId The ID of the trip whose documents are shown.
 * @param innerPadding Padding values provided by the parent [androidx.compose.material3.Scaffold].
 * @param onNavigateToDocument Called with the document ID when the user selects "Info" for a document.
 */
@Composable
internal fun TripDocumentsTabContent(
    tripId: Int,
    innerPadding: PaddingValues,
    onNavigateToDocument: (Int) -> Unit = {},
    viewModel: TripDocumentsViewModel = koinViewModel(
        key = "TripDocumentsViewModel:$tripId",
        parameters = { parametersOf(tripId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val suggestNameState by viewModel.suggestNameState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Exit selection mode on back press when selection is active.
    val isSelectionMode = (uiState as? TripDocumentsUiState.Success)?.let {
        it.selectedDocumentIds.isNotEmpty() || it.selectedFolderIds.isNotEmpty()
    } == true
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    // Navigate up one folder level on back press when inside a subfolder (and not in selection mode).
    val isInFolder = (uiState as? TripDocumentsUiState.Success)?.currentFolder != null
    BackHandler(enabled = isInFolder && !isSelectionMode) {
        viewModel.navigateUp()
    }

    // Pending file waiting for a name confirmation before being added to the trip.
    // Each field is saved separately so the confirmation dialog survives configuration changes.
    var pendingSourceUri by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingMimeType by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingOriginalName by rememberSaveable { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mimeType = context.contentResolver.getType(uri) ?: "*/*"
        val fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        } ?: run {
            val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "file"
            "document.$ext"
        }
        // Show a name confirmation dialog instead of adding immediately.
        pendingSourceUri = uri.toString()
        pendingMimeType = mimeType
        pendingOriginalName = fileName
    }

    // Name confirmation dialog shown after the file picker returns.
    val pendingUri = pendingSourceUri
    val pendingMime = pendingMimeType
    val pendingName = pendingOriginalName
    val isAiAvailable = (uiState as? TripDocumentsUiState.Success)?.isAiAvailable ?: false
    if (pendingUri != null && pendingMime != null && pendingName != null) {
        DocumentNameInputDialog(
            title = stringResource(R.string.documents_name_document_title),
            label = stringResource(R.string.documents_name_label),
            initialName = pendingName,
            suggestNameState = suggestNameState,
            onSuggest = { viewModel.requestSuggestName(pendingUri, pendingMime, null) },
            onConfirm = { name ->
                pendingSourceUri = null
                pendingMimeType = null
                pendingOriginalName = null
                viewModel.clearSuggestName()
                viewModel.addDocument(name = name, sourceUri = pendingUri, mimeType = pendingMime)
            },
            onDismiss = {
                pendingSourceUri = null
                pendingMimeType = null
                pendingOriginalName = null
                viewModel.clearSuggestName()
            },
            isAiAvailable = isAiAvailable,
        )
    }

    TripDocumentsContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onOpenFolder = viewModel::openFolder,
        onNavigateUp = viewModel::navigateUp,
        onCreateFolder = viewModel::createFolder,
        onRenameFolder = viewModel::renameFolder,
        onDeleteFolder = viewModel::removeFolder,
        onRenameDocument = viewModel::renameDocument,
        onMoveDocument = viewModel::moveDocument,
        onDeleteDocument = viewModel::removeDocument,
        onUploadFile = { filePicker.launch(arrayOf("*/*")) },
        onViewDocumentInfo = onNavigateToDocument,
        onErrorDismiss = viewModel::clearError,
        onToggleDocumentSelection = viewModel::toggleDocumentSelection,
        onToggleFolderSelection = viewModel::toggleFolderSelection,
        onSelectAll = viewModel::selectAll,
        onClearSelection = viewModel::clearSelection,
        onDeleteSelectedDocuments = viewModel::deleteSelectedDocuments,
        onMoveSelectedDocuments = viewModel::moveSelectedDocuments,
        suggestNameState = suggestNameState,
        onRequestSuggestName = viewModel::requestSuggestName,
        onClearSuggestName = viewModel::clearSuggestName,
        onAutoOrganize = viewModel::requestAutoOrganize,
        onConfirmAutoOrganize = viewModel::applyOrganization,
        onCancelAutoOrganize = viewModel::cancelAutoOrganize,
    )
}

/**
 * Stateless presentation of the Documents tab content.
 *
 * Supports folder navigation, create/rename/delete dialogs, document rename/delete/move,
 * file upload (via [onUploadFile]).
 * [onErrorDismiss] is called when the user acknowledges a transient writing error.
 * Multi-select mode is entered by long-pressing a document or folder row;
 * [onToggleDocumentSelection], [onToggleFolderSelection], [onSelectAll], [onClearSelection],
 * [onDeleteSelectedDocuments], and [onMoveSelectedDocuments] handle the bulk-operation actions.
 *
 * [suggestNameState], [onRequestSuggestName], and [onClearSuggestName] drive the AI name
 * suggestion feature inside the rename-document dialog.
 *
 * [onAutoOrganize] triggers the AI auto-organize flow. [onConfirmAutoOrganize] applies the
 * suggested plan; [onCancelAutoOrganize] dismisses the dialog without making any changes.
 */
@Composable
internal fun TripDocumentsContent(
    uiState: TripDocumentsUiState,
    innerPadding: PaddingValues,
    onOpenFolder: (TripDocumentFolder) -> Unit = {},
    onNavigateUp: () -> Unit = {},
    onCreateFolder: (String) -> Unit = {},
    onRenameFolder: (TripDocumentFolder, String) -> Unit = { _, _ -> },
    onDeleteFolder: (TripDocumentFolder) -> Unit = {},
    onRenameDocument: (TripDocument, String) -> Unit = { _, _ -> },
    onMoveDocument: (TripDocument, Int?) -> Unit = { _, _ -> },
    onDeleteDocument: (TripDocument) -> Unit = {},
    onUploadFile: () -> Unit = {},
    onViewDocumentInfo: (Int) -> Unit = {},
    onErrorDismiss: () -> Unit = {},
    onToggleDocumentSelection: (TripDocument) -> Unit = {},
    onToggleFolderSelection: (TripDocumentFolder) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelectedDocuments: () -> Unit = {},
    onMoveSelectedDocuments: (targetFolderId: Int?) -> Unit = {},
    suggestNameState: SuggestNameUiState? = null,
    onRequestSuggestName: (fileUri: String, mimeType: String, excludeName: String?) -> Unit = { _, _, _ -> },
    onClearSuggestName: () -> Unit = {},
    onAutoOrganize: () -> Unit = {},
    onConfirmAutoOrganize: (OrganizationPlan) -> Unit = {},
    onCancelAutoOrganize: () -> Unit = {},
) {
    var isFabExpanded by rememberSaveable { mutableStateOf(false) }
    var showCreateFolderDialog by rememberSaveable { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<TripDocumentFolder?>(null) }
    var folderToDelete by remember { mutableStateOf<TripDocumentFolder?>(null) }
    var documentToRename by remember { mutableStateOf<TripDocument?>(null) }
    var documentToMove by remember { mutableStateOf<TripDocument?>(null) }
    var documentToDelete by remember { mutableStateOf<TripDocument?>(null) }
    var showDeleteSelectedDialog by rememberSaveable { mutableStateOf(false) }
    var showMoveSelectedDialog by rememberSaveable { mutableStateOf(false) }

    val successState = uiState as? TripDocumentsUiState.Success
    val isAiAvailable = successState?.isAiAvailable ?: false
    val autoOrganizeState = successState?.autoOrganizeState
    // At root level the AI analyzes all trip documents (including those inside folders), so
    // check the full-trip count; inside a folder only that folder's documents are organized.
    val hasDocuments = if (successState?.currentFolder == null) {
        (successState?.allDocuments?.size ?: 0) >= 2
    } else {
        (successState.documents.size) >= 2
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is TripDocumentsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is TripDocumentsUiState.Success -> {
                val isSelectionMode = uiState.selectedDocumentIds.isNotEmpty() || uiState.selectedFolderIds.isNotEmpty()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    when {
                        // Selection mode: show contextual action bar instead of breadcrumb
                        isSelectionMode -> {
                            SelectionBar(
                                selectedCount = uiState.selectedDocumentIds.size + uiState.selectedFolderIds.size,
                                totalCount = uiState.documents.size + uiState.folders.size,
                                onClearSelection = onClearSelection,
                                onSelectAll = onSelectAll,
                            )
                            HorizontalDivider()
                        }
                        // Breadcrumb / back row when inside a subfolder (normal mode)
                        uiState.currentFolder != null -> {
                            val parentName = uiState.folderStack
                                .dropLast(1)
                                .lastOrNull()
                                ?.name
                            val backLabel = if (parentName != null) {
                                stringResource(R.string.documents_back_to_folder, parentName)
                            } else {
                                stringResource(R.string.documents_back_to_root)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = onNavigateUp) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = backLabel,
                                    )
                                }
                                Text(
                                    text = uiState.folderStack
                                        .joinToString(" / ") { it.name }
                                        .ifEmpty { uiState.currentFolder.name },
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            HorizontalDivider()
                        }
                    }

                    val isEmpty = uiState.folders.isEmpty() && uiState.documents.isEmpty()
                    if (isEmpty) {
                        val emptySubtitle = if (uiState.currentFolder == null) {
                            stringResource(R.string.documents_empty_root_subtitle)
                        } else {
                            stringResource(R.string.documents_empty_folder_subtitle)
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = stringResource(R.string.documents_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = emptySubtitle,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                            )
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(uiState.folders, key = { "folder-${it.id}" }) { folder ->
                                FolderRow(
                                    folder = folder,
                                    isSelected = folder.id in uiState.selectedFolderIds,
                                    isSelectionMode = isSelectionMode,
                                    onClick = {
                                        if (isSelectionMode) onToggleFolderSelection(folder)
                                        else onOpenFolder(folder)
                                    },
                                    onLongClick = { onToggleFolderSelection(folder) },
                                    onRename = { folderToRename = folder },
                                    onDelete = { folderToDelete = folder },
                                )
                                HorizontalDivider()
                            }
                            items(uiState.documents, key = { "doc-${it.id}" }) { document ->
                                DocumentRow(
                                    document = document,
                                    onViewInfo = { onViewDocumentInfo(document.id) },
                                    onRename = { documentToRename = document },
                                    onMove = { documentToMove = document },
                                    onDelete = { documentToDelete = document },
                                    isSelected = document.id in uiState.selectedDocumentIds,
                                    isSelectionMode = isSelectionMode,
                                    onToggleSelect = { onToggleDocumentSelection(document) },
                                )
                                HorizontalDivider()
                            }
                        }
                    }

                    // Multi-select action bar shown at the bottom when in selection mode.
                    if (isSelectionMode) {
                        HorizontalDivider()
                        MultiSelectActionBar(
                            onDeleteSelected = { showDeleteSelectedDialog = true },
                            onMoveSelected = { showMoveSelectedDialog = true },
                            showMoveAction = uiState.selectedFolderIds.isEmpty(),
                        )
                    }
                }

                // Speed-dial FAB — hidden in selection mode.
                if (!isSelectionMode) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        AnimatedVisibility(
                            visible = isFabExpanded,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut(),
                        ) {
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                SpeedDialItem(
                                    label = stringResource(R.string.documents_upload_file),
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.UploadFile,
                                            contentDescription = stringResource(R.string.documents_upload_file),
                                        )
                                    },
                                    onClick = {
                                        isFabExpanded = false
                                        onUploadFile()
                                    },
                                )
                                SpeedDialItem(
                                    label = stringResource(R.string.documents_add_folder),
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.FolderOpen,
                                            contentDescription = stringResource(R.string.documents_add_folder),
                                        )
                                    },
                                    onClick = {
                                        isFabExpanded = false
                                        showCreateFolderDialog = true
                                    },
                                )
                                if (isAiAvailable && hasDocuments) {
                                    SpeedDialItem(
                                        label = stringResource(R.string.documents_auto_organize),
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = stringResource(R.string.documents_auto_organize),
                                            )
                                        },
                                        onClick = {
                                            isFabExpanded = false
                                            onAutoOrganize()
                                        },
                                    )
                                }
                            }
                        }
                        FloatingActionButton(
                            onClick = { isFabExpanded = !isFabExpanded },
                        ) {
                            Icon(
                                imageVector = if (isFabExpanded) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = if (isFabExpanded) {
                                    stringResource(R.string.dialog_cancel)
                                } else {
                                    stringResource(R.string.documents_add_action)
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (showCreateFolderDialog) {
        NameInputDialog(
            title = stringResource(R.string.documents_new_folder_title),
            label = stringResource(R.string.documents_folder_name_label),
            initialName = "",
            onConfirm = { name ->
                showCreateFolderDialog = false
                onCreateFolder(name)
            },
            onDismiss = { showCreateFolderDialog = false },
        )
    }

    folderToRename?.let { folder ->
        NameInputDialog(
            title = stringResource(R.string.documents_rename_folder_title),
            label = stringResource(R.string.documents_folder_name_label),
            initialName = folder.name,
            onConfirm = { newName ->
                folderToRename = null
                onRenameFolder(folder, newName)
            },
            onDismiss = { folderToRename = null },
        )
    }

    folderToDelete?.let { folder ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.documents_delete_folder_title),
            message = stringResource(R.string.documents_delete_folder_message, folder.name),
            onConfirm = {
                folderToDelete = null
                onDeleteFolder(folder)
            },
            onDismiss = { folderToDelete = null },
        )
    }

    documentToRename?.let { document ->
        DocumentNameInputDialog(
            title = stringResource(R.string.documents_rename_document_title),
            label = stringResource(R.string.documents_name_label),
            initialName = document.name,
            suggestNameState = suggestNameState,
            onSuggest = { onRequestSuggestName(document.uri, document.mimeType, document.name) },
            onConfirm = { newName ->
                documentToRename = null
                onClearSuggestName()
                onRenameDocument(document, newName)
            },
            onDismiss = {
                documentToRename = null
                onClearSuggestName()
            },
            isAiAvailable = isAiAvailable,
        )
    }

    documentToMove?.let { document ->
        val allFolders = (uiState as? TripDocumentsUiState.Success)?.allFolders ?: emptyList()
        MoveFolderPickerDialog(
            allFolders = allFolders,
            onMove = { targetFolderId ->
                documentToMove = null
                onMoveDocument(document, targetFolderId)
            },
            onDismiss = { documentToMove = null },
        )
    }

    documentToDelete?.let { document ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.documents_delete_document_title),
            message = stringResource(R.string.documents_delete_document_message, document.name),
            onConfirm = {
                documentToDelete = null
                onDeleteDocument(document)
            },
            onDismiss = { documentToDelete = null },
        )
    }

    // Multi-select: confirm bulk delete
    if (showDeleteSelectedDialog) {
        val successState = uiState as? TripDocumentsUiState.Success
        val count = (successState?.selectedDocumentIds?.size ?: 0) + (successState?.selectedFolderIds?.size ?: 0)
        ConfirmDeleteDialog(
            title = stringResource(R.string.documents_delete_selected_title),
            message = pluralStringResource(R.plurals.documents_delete_selected_message, count, count),
            onConfirm = {
                showDeleteSelectedDialog = false
                onDeleteSelectedDocuments()
            },
            onDismiss = { showDeleteSelectedDialog = false },
        )
    }

    // Multi-select: move all selected documents to a chosen folder
    if (showMoveSelectedDialog) {
        val allFolders = (uiState as? TripDocumentsUiState.Success)?.allFolders ?: emptyList()
        MoveFolderPickerDialog(
            allFolders = allFolders,
            onMove = { targetFolderId ->
                showMoveSelectedDialog = false
                onMoveSelectedDocuments(targetFolderId)
            },
            onDismiss = { showMoveSelectedDialog = false },
        )
    }

    // Show a dialog when a write operation fails (e.g. duplicate name).
    val writeError = (uiState as? TripDocumentsUiState.Success)?.writeError
    if (writeError != null) {
        val errorText = when (writeError) {
            DocumentsWriteError.DuplicateName -> stringResource(R.string.documents_duplicate_name_error)
            DocumentsWriteError.Generic -> stringResource(R.string.documents_write_error)
        }
        AlertDialog(
            onDismissRequest = onErrorDismiss,
            title = { Text(stringResource(R.string.documents_write_error_title)) },
            text = { Text(errorText) },
            confirmButton = {
                TextButton(onClick = onErrorDismiss) {
                    Text(stringResource(R.string.dialog_ok))
                }
            },
        )
    }

    // Auto-organize: single dialog covering all states (loading/downloading/result/error).
    // A single persistent dialog is used to avoid the onDismissRequest bug that fires when
    // an AlertDialog is removed from composition (see DocumentAnalysisComponents for details).
    if (autoOrganizeState != null) {
        AutoOrganizeDialog(
            state = autoOrganizeState,
            onConfirm = onConfirmAutoOrganize,
            onDismiss = onCancelAutoOrganize,
        )
    }
}

// ── Speed dial helper ─────────────────────────────────────────────────────────

@Composable
private fun SpeedDialItem(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Card(
            modifier = Modifier.clickable(onClick = onClick),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        SmallFloatingActionButton(onClick = onClick) {
            icon()
        }
    }
}

// ── Auto-organize dialog ──────────────────────────────────────────────────────

/**
 * A single unified dialog for the auto-organize flow, adapting its content to [state]:
 * - [AutoOrganizeUiState.Loading] / [AutoOrganizeUiState.Downloading]: progress indicator.
 *   Back-press and outside-tap are disabled while work is in progress; an explicit Cancel
 *   button is shown to allow the user to abort.
 * - [AutoOrganizeUiState.ReadyToConfirm]: scrollable preview of the proposed folder structure.
 * - [AutoOrganizeUiState.Unavailable] / [AutoOrganizeUiState.Error]: status message.
 *
 * [onConfirm] is only called when [state] is [AutoOrganizeUiState.ReadyToConfirm].
 * [onDismiss] is called to cancel or close the dialog from any state.
 */
@Composable
private fun AutoOrganizeDialog(
    state: AutoOrganizeUiState,
    onConfirm: (OrganizationPlan) -> Unit,
    onDismiss: () -> Unit,
) {
    val titleRes = when (state) {
        is AutoOrganizeUiState.ReadyToConfirm -> R.string.documents_auto_organize_confirm_title
        else -> R.string.documents_auto_organize
    }
    val isInProgress = state is AutoOrganizeUiState.Loading || state is AutoOrganizeUiState.Downloading

    AlertDialog(
        onDismissRequest = { if (!isInProgress) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = !isInProgress,
            dismissOnClickOutside = !isInProgress,
        ),
        title = { Text(stringResource(titleRes)) },
        text = {
            when (state) {
                is AutoOrganizeUiState.Loading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.documents_auto_organize_loading))
                    }
                }
                is AutoOrganizeUiState.Downloading -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        Text(
                            stringResource(
                                R.string.documents_auto_organize_downloading,
                                formatBytes(state.bytesDownloaded),
                            ),
                        )
                    }
                }
                is AutoOrganizeUiState.ReadyToConfirm -> {
                    AutoOrganizePlanPreview(plan = state.plan)
                }
                is AutoOrganizeUiState.Unavailable -> {
                    Text(stringResource(R.string.documents_auto_organize_unavailable))
                }
                is AutoOrganizeUiState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.documents_auto_organize_error))
                        val message = state.message
                        if (!message.isNullOrBlank()) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (state is AutoOrganizeUiState.ReadyToConfirm) {
                TextButton(onClick = { onConfirm(state.plan) }) {
                    Text(stringResource(R.string.documents_auto_organize_apply))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

/**
 * Scrollable preview of the proposed [OrganizationPlan], listing each folder and its documents.
 */
@Composable
private fun AutoOrganizePlanPreview(plan: OrganizationPlan) {
    if (plan.folderAssignments.isEmpty()) {
        Text(stringResource(R.string.documents_auto_organize_no_suggestion))
        return
    }
    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
        plan.folderAssignments.forEachIndexed { folderIndex, assignment ->
            item(key = "folder-header-$folderIndex") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = assignment.folderName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            items(
                items = assignment.documents,
                key = { document -> "folder-$folderIndex-doc-${document.id}" },
            ) { document ->
                Text(
                    text = "  • ${document.name}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 24.dp, top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Formats a byte count as a human-readable string (e.g. "1.2 MB"). */
private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
    bytes >= 1_024 -> "%.0f KB".format(bytes / 1_024.0)
    else -> "$bytes B"
}

// ── Rows ──────────────────────────────────────────────────────────────────────

/**
 * Contextual action bar shown at the top of the list when multi-select mode is active.
 *
 * Displays the number of selected documents, a "close" button to exit selection mode,
 * and a select-all/deselect-all toggle: "select all" when not all documents are selected,
 * "deselect all" (calls [onClearSelection]) when all documents are already selected.
 */
@Composable
private fun SelectionBar(
    selectedCount: Int,
    totalCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClearSelection) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.documents_selection_clear),
            )
        }
        Text(
            text = pluralStringResource(R.plurals.documents_selection_count, selectedCount, selectedCount),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = if (selectedCount == totalCount && totalCount > 0) onClearSelection else onSelectAll) {
            Icon(
                imageVector = if (selectedCount == totalCount && totalCount > 0) {
                    Icons.Default.CheckBox
                } else {
                    Icons.Default.SelectAll
                },
                contentDescription = if (selectedCount == totalCount && totalCount > 0) {
                    stringResource(R.string.documents_selection_clear)
                } else {
                    stringResource(R.string.documents_select_all)
                },
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Persistent bottom action bar shown when multi-select mode is active.
 * Provides bulk Delete and Move actions for the selected documents.
 * [showMoveAction] controls whether the Move button is visible; it is hidden when folders
 * are selected since moving folders is not supported via the bulk-move flow.
 */
@Composable
private fun MultiSelectActionBar(
    onDeleteSelected: () -> Unit,
    onMoveSelected: () -> Unit,
    showMoveAction: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TextButton(onClick = onDeleteSelected) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(R.string.documents_delete_action))
            }
            if (showMoveAction) {
                TextButton(onClick = onMoveSelected) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.DriveFileMove,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.documents_move_action))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderRow(
    folder: TripDocumentFolder,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onRename: () -> Unit,
    onDelete: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .then(
                if (isSelectionMode) {
                    Modifier.semantics(mergeDescendants = true) {
                        selected = isSelected
                        role = Role.Checkbox
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelectionMode) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        } else {
            IconButton(onClick = onClick) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = stringResource(R.string.documents_folder_content_desc, folder.name),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!isSelectionMode) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.documents_more_options),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.documents_rename_action)) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.documents_delete_action)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentRow(
    document: TripDocument,
    onViewInfo: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    onToggleSelect: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelect() else onViewInfo() },
                onLongClick = { onToggleSelect() },
            )
            .then(
                if (isSelectionMode) {
                    Modifier.semantics(mergeDescendants = true) {
                        selected = isSelected
                        role = Role.Checkbox
                    }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelectionMode) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        } else {
            IconButton(onClick = onViewInfo) {
                Icon(
                    imageVector = documentTypeIcon(document.mimeType),
                    contentDescription = stringResource(R.string.documents_view_info_content_desc, document.name),
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
        Text(
            text = document.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!isSelectionMode) {
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.documents_more_options),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.documents_rename_action)) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.documents_move_action)) },
                        onClick = {
                            menuExpanded = false
                            onMove()
                        },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.documents_delete_action)) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                    )
                }
            }
        }
    }
}

// ── Dialog helpers ────────────────────────────────────────────────────────────

@Composable
private fun NameInputDialog(
    title: String,
    label: String,
    initialName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(label) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

/**
 * A name-input dialog with an optional AI filename suggestion icon.
 *
 * The suggestion is triggered via a trailing icon inside the name text field. While a suggestion
 * is in-flight the icon is replaced by a progress indicator; when [SuggestNameUiState.Success]
 * arrives the text field is updated automatically.
 */
@Composable
private fun DocumentNameInputDialog(
    title: String,
    label: String,
    initialName: String,
    suggestNameState: SuggestNameUiState?,
    onSuggest: () -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isAiAvailable: Boolean = true,
) {
    var text by rememberSaveable(initialName) { mutableStateOf(initialName) }

    // Update the text field when a new suggestion arrives.
    LaunchedEffect(suggestNameState) {
        if (suggestNameState is SuggestNameUiState.Success) {
            text = suggestNameState.suggestedName
        }
    }

    val isSuggesting = suggestNameState is SuggestNameUiState.Loading ||
        suggestNameState is SuggestNameUiState.Downloading

    val suggestLoadingDesc = stringResource(R.string.documents_suggest_name_loading)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(label) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = if (!isAiAvailable) null else {
                        {
                            if (isSuggesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .semantics {
                                            contentDescription = suggestLoadingDesc
                                        },
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                IconButton(
                                    onClick = onSuggest,
                                    enabled = suggestNameState !is SuggestNameUiState.Unavailable,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = stringResource(R.string.documents_suggest_name),
                                        tint = if (suggestNameState is SuggestNameUiState.Unavailable ||
                                            suggestNameState is SuggestNameUiState.Error
                                        ) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                    )
                                }
                            }
                        }
                    },
                )
                val statusText = when (suggestNameState) {
                    is SuggestNameUiState.Loading ->
                        stringResource(R.string.documents_suggest_name_loading)
                    is SuggestNameUiState.Downloading ->
                        stringResource(R.string.documents_suggest_name_downloading)
                    is SuggestNameUiState.Unavailable ->
                        stringResource(R.string.documents_suggest_name_unavailable)
                    is SuggestNameUiState.Error ->
                        stringResource(R.string.documents_suggest_name_error)
                    else -> null
                }
                if (statusText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (suggestNameState is SuggestNameUiState.Unavailable ||
                            suggestNameState is SuggestNameUiState.Error
                        ) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank(),
            ) {
                Text(stringResource(R.string.dialog_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun ConfirmDeleteDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.documents_delete_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

/**
 * A dialog that lets the user pick a destination folder when moving a document.
 *
 * Shows a "Root (no folder)" option followed by all folders in the trip. Each folder is labelled
 * with its full ancestor path (e.g. "Travel / Documents") to disambiguate folders that share the
 * same name under different parents. Tapping an option immediately calls [onMove] with the
 * selected folder ID (or `null` for root) and dismisses the dialog.
 */
@Composable
private fun MoveFolderPickerDialog(
    allFolders: List<TripDocumentFolder>,
    onMove: (targetFolderId: Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val folderMap = remember(allFolders) { allFolders.associateBy { it.id } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.documents_move_document_title)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                // Root option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onMove(null) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.documents_move_to_root),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                allFolders.forEach { folder ->
                    HorizontalDivider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMove(folder.id) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = buildFolderPath(folder, folderMap),
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
        // No confirmation button — the dialog is dismissed automatically on item selection.
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

/**
 * Builds a human-readable path for [folder] by walking up the [folderMap] via [TripDocumentFolder.parentFolderId].
 * Example: "Travel / Flights / Tickets".
 *
 * A visited-ID set guards against cycles in case of data corruption, so the loop always terminates.
 */
private fun buildFolderPath(
    folder: TripDocumentFolder,
    folderMap: Map<Int, TripDocumentFolder>,
): String {
    val parts = mutableListOf<String>()
    val visited = mutableSetOf<Int>()
    var current: TripDocumentFolder? = folder
    while (current != null && visited.add(current.id)) {
        parts.add(current.name)
        current = current.parentFolderId?.let { folderMap[it] }
    }
    parts.reverse()
    return parts.joinToString(" / ")
}

/**
 * Returns the icon that best represents the given [mimeType].
 *
 * @param mimeType The MIME type string of the document (e.g. "application/pdf", "image/jpeg").
 * @return An [ImageVector] icon: [Icons.Default.PictureAsPdf] for PDF,
 *   [Icons.Default.Image] for images, [Icons.Default.TextSnippet] for text files,
 *   [Icons.Default.Videocam] for video, [Icons.Default.MusicNote] for audio,
 *   or [Icons.Default.Description] as a fallback for all other types.
 */
private fun documentTypeIcon(mimeType: String): ImageVector = when {
    mimeType == "application/pdf" -> Icons.Default.PictureAsPdf
    mimeType.startsWith("image/") -> Icons.Default.Image
    mimeType.startsWith("text/") -> Icons.Default.TextSnippet
    mimeType.startsWith("video/") -> Icons.Default.Videocam
    mimeType.startsWith("audio/") -> Icons.Default.MusicNote
    else -> Icons.Default.Description
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun TripDocumentsEmptyPreview() {
    WanderVaultTheme {
        TripDocumentsContent(
            uiState = TripDocumentsUiState.Success(),
            innerPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDocumentsFolderListPreview() {
    val folders = listOf(
        TripDocumentFolder(id = 1, tripId = 1, name = "Flight Tickets"),
        TripDocumentFolder(id = 2, tripId = 1, name = "Hotel Bookings"),
        TripDocumentFolder(id = 3, tripId = 1, name = "Insurance"),
    )
    WanderVaultTheme {
        TripDocumentsContent(
            uiState = TripDocumentsUiState.Success(folders = folders),
            innerPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDocumentsDocumentListPreview() {
    val folder = TripDocumentFolder(id = 1, tripId = 1, name = "Flight Tickets")
    val documents = listOf(
        TripDocument(id = 1, tripId = 1, folderId = 1, name = "outbound.pdf", uri = "", mimeType = "application/pdf"),
        TripDocument(id = 2, tripId = 1, folderId = 1, name = "return.pdf", uri = "", mimeType = "application/pdf"),
    )
    WanderVaultTheme {
        TripDocumentsContent(
            uiState = TripDocumentsUiState.Success(
                documents = documents,
                currentFolder = folder,
                folderStack = listOf(folder),
            ),
            innerPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDocumentsNestedFolderPathPreview() {
    val root = TripDocumentFolder(id = 1, tripId = 1, name = "Travel")
    val mid = TripDocumentFolder(id = 2, tripId = 1, name = "Flights", parentFolderId = 1)
    val leaf = TripDocumentFolder(id = 3, tripId = 1, name = "Tickets", parentFolderId = 2)
    WanderVaultTheme {
        TripDocumentsContent(
            uiState = TripDocumentsUiState.Success(
                currentFolder = leaf,
                folderStack = listOf(root, mid, leaf),
            ),
            innerPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDocumentsRootWithDocumentsPreview() {
    val documents = listOf(
        TripDocument(id = 1, tripId = 1, name = "photo.jpg", uri = "", mimeType = "image/jpeg"),
        TripDocument(id = 2, tripId = 1, name = "travel_insurance.pdf", uri = "", mimeType = "application/pdf"),
        TripDocument(id = 3, tripId = 1, name = "itinerary.txt", uri = "", mimeType = "text/plain"),
        TripDocument(id = 4, tripId = 1, name = "promo_video.mp4", uri = "", mimeType = "video/mp4"),
        TripDocument(id = 5, tripId = 1, name = "audio_guide.mp3", uri = "", mimeType = "audio/mpeg"),
        TripDocument(id = 6, tripId = 1, name = "other_doc.docx", uri = "", mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    )
    val folders = listOf(
        TripDocumentFolder(id = 1, tripId = 1, name = "Flight Tickets"),
    )
    WanderVaultTheme {
        TripDocumentsContent(
            uiState = TripDocumentsUiState.Success(
                documents = documents,
                folders = folders,
            ),
            innerPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDocumentsSelectionModePreview() {
    val documents = listOf(
        TripDocument(id = 1, tripId = 1, name = "passport_scan.pdf", uri = "", mimeType = "application/pdf"),
        TripDocument(id = 2, tripId = 1, name = "travel_insurance.pdf", uri = "", mimeType = "application/pdf"),
        TripDocument(id = 3, tripId = 1, name = "flight_ticket.pdf", uri = "", mimeType = "application/pdf"),
    )
    WanderVaultTheme {
        TripDocumentsContent(
            uiState = TripDocumentsUiState.Success(
                documents = documents,
                selectedDocumentIds = setOf(1, 3),
            ),
            innerPadding = PaddingValues(0.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDocumentsFolderSelectionModePreview() {
    val folders = listOf(
        TripDocumentFolder(id = 1, tripId = 1, name = "Flight Tickets"),
        TripDocumentFolder(id = 2, tripId = 1, name = "Hotel Bookings"),
        TripDocumentFolder(id = 3, tripId = 1, name = "Insurance"),
    )
    val documents = listOf(
        TripDocument(id = 1, tripId = 1, name = "passport_scan.pdf", uri = "", mimeType = "application/pdf"),
    )
    WanderVaultTheme {
        TripDocumentsContent(
            uiState = TripDocumentsUiState.Success(
                folders = folders,
                documents = documents,
                selectedFolderIds = setOf(1, 3),
            ),
            innerPadding = PaddingValues(0.dp),
        )
    }
}
