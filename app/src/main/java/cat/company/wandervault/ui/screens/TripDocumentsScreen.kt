package cat.company.wandervault.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
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
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.UploadFile
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Documents tab entry point for the Trip Detail screen.
 *
 * @param tripId The ID of the trip whose documents are shown.
 * @param innerPadding Padding values provided by the parent [androidx.compose.material3.Scaffold].
 */
@Composable
internal fun TripDocumentsTabContent(
    tripId: Int,
    innerPadding: PaddingValues,
    viewModel: TripDocumentsViewModel = koinViewModel(
        key = "TripDocumentsViewModel:$tripId",
        parameters = { parametersOf(tripId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val noAppMessage = stringResource(R.string.documents_no_app_to_open)

    // Exit selection mode on back press when selection is active.
    val isSelectionMode = (uiState as? TripDocumentsUiState.Success)?.selectedDocumentIds?.isNotEmpty() == true
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    // Navigate up one folder level on back press when inside a subfolder (and not in selection mode).
    val isInFolder = (uiState as? TripDocumentsUiState.Success)?.currentFolder != null
    BackHandler(enabled = isInFolder && !isSelectionMode) {
        viewModel.navigateUp()
    }

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
        viewModel.addDocument(
            name = fileName,
            sourceUri = uri.toString(),
            mimeType = mimeType,
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
        onOpenDocument = { document ->
            try {
                val uri = document.uri.toUri()
                val contentUri = if (uri.scheme == "file") {
                    val path = uri.path
                        ?: throw IllegalArgumentException("File URI has no path: $uri")
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        File(path),
                    )
                } else {
                    uri
                }
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, document.mimeType.ifBlank { "*/*" })
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                android.widget.Toast.makeText(context, noAppMessage, android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: IllegalArgumentException) {
                android.widget.Toast.makeText(context, noAppMessage, android.widget.Toast.LENGTH_SHORT).show()
            }
        },
        onAnalyzeDocument = viewModel::analyzeDocument,
        onAnalyzeApplyChanges = viewModel::applyAnalysisChanges,
        onAnalyzeFlightLegSelected = viewModel::onFlightLegSelected,
        onAnalyzeFlightConfirmed = viewModel::onFlightConfirmed,
        onAnalyzeHotelDestinationSelected = viewModel::onHotelDestinationSelected,
        onAnalyzeHotelConfirmed = viewModel::onHotelConfirmed,
        onAnalyzeDismiss = viewModel::dismissAnalyze,
        onErrorDismiss = viewModel::clearError,
        onToggleDocumentSelection = viewModel::toggleDocumentSelection,
        onSelectAllDocuments = viewModel::selectAllDocuments,
        onClearSelection = viewModel::clearSelection,
        onDeleteSelectedDocuments = viewModel::deleteSelectedDocuments,
        onMoveSelectedDocuments = viewModel::moveSelectedDocuments,
    )
}

/**
 * Stateless presentation of the Documents tab content.
 *
 * Supports folder navigation, create/rename/delete dialogs, document rename/delete/move/analyze,
 * file upload (via [onUploadFile]), and document open (via [onOpenDocument]).
 * [onErrorDismiss] is called when the user acknowledges a transient writing error.
 * Multi-select mode is entered by long-pressing a document row; [onToggleDocumentSelection],
 * [onSelectAllDocuments], [onClearSelection], [onDeleteSelectedDocuments], and
 * [onMoveSelectedDocuments] handle the bulk-operation actions.
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
    onOpenDocument: (TripDocument) -> Unit = {},
    onAnalyzeDocument: (TripDocument) -> Unit = {},
    onAnalyzeApplyChanges: () -> Unit = {},
    onAnalyzeFlightLegSelected: (TransportLeg) -> Unit = {},
    onAnalyzeFlightConfirmed: () -> Unit = {},
    onAnalyzeHotelDestinationSelected: (Destination) -> Unit = {},
    onAnalyzeHotelConfirmed: () -> Unit = {},
    onAnalyzeDismiss: () -> Unit = {},
    onErrorDismiss: () -> Unit = {},
    onToggleDocumentSelection: (TripDocument) -> Unit = {},
    onSelectAllDocuments: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onDeleteSelectedDocuments: () -> Unit = {},
    onMoveSelectedDocuments: (targetFolderId: Int?) -> Unit = {},
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

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is TripDocumentsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is TripDocumentsUiState.Success -> {
                val isSelectionMode = uiState.selectedDocumentIds.isNotEmpty()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    when {
                        // Selection mode: show contextual action bar instead of breadcrumb
                        isSelectionMode -> {
                            SelectionBar(
                                selectedCount = uiState.selectedDocumentIds.size,
                                totalCount = uiState.documents.size,
                                onClearSelection = onClearSelection,
                                onSelectAll = onSelectAllDocuments,
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
                                    onClick = { if (!isSelectionMode) onOpenFolder(folder) },
                                    onRename = { folderToRename = folder },
                                    onDelete = { folderToDelete = folder },
                                )
                                HorizontalDivider()
                            }
                            items(uiState.documents, key = { "doc-${it.id}" }) { document ->
                                DocumentRow(
                                    document = document,
                                    onOpen = { onOpenDocument(document) },
                                    onRename = { documentToRename = document },
                                    onMove = { documentToMove = document },
                                    onDelete = { documentToDelete = document },
                                    onAnalyze = { onAnalyzeDocument(document) },
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
        NameInputDialog(
            title = stringResource(R.string.documents_rename_document_title),
            label = stringResource(R.string.documents_name_label),
            initialName = document.name,
            onConfirm = { newName ->
                documentToRename = null
                onRenameDocument(document, newName)
            },
            onDismiss = { documentToRename = null },
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
        val count = (uiState as? TripDocumentsUiState.Success)?.selectedDocumentIds?.size ?: 0
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

    // Show the unified analysis dialog whenever an analysis is active. A single AlertDialog
    // composable is used for all states (Loading, Downloading, Result, FlightConfirm,
    // HotelConfirm, FlightLegSelection, HotelDestinationSelection, Unavailable, Error) so
    // that the same dialog window persists throughout the entire analysis flow. Replacing the
    // dialog composable with a different one during a state transition (e.g. Result →
    // FlightConfirm) would trigger the old dialog's onDismissRequest via the underlying
    // android.app.Dialog.dismiss(), which would call dismissAnalyze() and prevent the
    // confirmation dialog from being shown.
    val analyzeState = (uiState as? TripDocumentsUiState.Success)?.analyzeState
    if (analyzeState != null) {
        AnalyzeDocumentDialog(
            analyzeState = analyzeState,
            onApplyChanges = onAnalyzeApplyChanges,
            onFlightLegSelected = onAnalyzeFlightLegSelected,
            onFlightConfirmed = onAnalyzeFlightConfirmed,
            onHotelDestinationSelected = onAnalyzeHotelDestinationSelected,
            onHotelConfirmed = onAnalyzeHotelConfirmed,
            onDismiss = onAnalyzeDismiss,
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
 */
@Composable
private fun MultiSelectActionBar(
    onDeleteSelected: () -> Unit,
    onMoveSelected: () -> Unit,
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

@Composable
private fun FolderRow(
    folder: TripDocumentFolder,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = stringResource(R.string.documents_folder_content_desc, folder.name),
                tint = MaterialTheme.colorScheme.primary,
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentRow(
    document: TripDocument,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onAnalyze: () -> Unit,
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
                onClick = { if (isSelectionMode) onToggleSelect() else onOpen() },
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
            IconButton(onClick = onOpen) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = stringResource(R.string.documents_open_content_desc, document.name),
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
                        text = { Text(stringResource(R.string.documents_analyze_action)) },
                        onClick = {
                            menuExpanded = false
                            onAnalyze()
                        },
                        leadingIcon = { Icon(Icons.Default.FindInPage, contentDescription = null) },
                    )
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
 * A single unified dialog that covers the entire document-analysis flow. Keeping one [AlertDialog]
 * composable alive for the lifetime of the analysis (rather than swapping between separate
 * composables per state) prevents a subtle Android bug: removing a Compose [AlertDialog] from the
 * composition calls [android.app.Dialog.dismiss] on the underlying window, which asynchronously
 * fires the [android.content.DialogInterface.OnDismissListener] and therefore also invokes
 * [onDismissRequest]. If a separate dialog were used for each state the transition
 * Result → FlightConfirm / HotelConfirm would silently call [onDismiss] → [TripDocumentsViewModel.dismissAnalyze],
 * clearing [AnalyzeDocumentUiState] and preventing the confirmation dialog from being displayed.
 *
 * The dialog title, body, confirm button and dismiss button all adapt to [analyzeState]:
 * - [AnalyzeDocumentUiState.Loading] / [AnalyzeDocumentUiState.Downloading]: progress indicator.
 * - [AnalyzeDocumentUiState.Result]: summary + optional "Apply Changes" button.
 * - [AnalyzeDocumentUiState.Unavailable] / [AnalyzeDocumentUiState.Error]: status message.
 * - [AnalyzeDocumentUiState.FlightConfirm] / [AnalyzeDocumentUiState.HotelConfirm]: extracted
 *   info alongside the matched leg / destination; "Confirm" button applies the changes.
 * - [AnalyzeDocumentUiState.FlightLegSelection] / [AnalyzeDocumentUiState.HotelDestinationSelection]:
 *   scrollable candidate list; tapping an item applies the changes and dismisses.
 */
@Composable
private fun AnalyzeDocumentDialog(
    analyzeState: AnalyzeDocumentUiState,
    onApplyChanges: () -> Unit,
    onFlightLegSelected: (TransportLeg) -> Unit,
    onFlightConfirmed: () -> Unit,
    onHotelDestinationSelected: (Destination) -> Unit,
    onHotelConfirmed: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    val titleRes = when (analyzeState) {
        is AnalyzeDocumentUiState.FlightLegSelection -> R.string.share_flight_selection_title
        is AnalyzeDocumentUiState.HotelDestinationSelection -> R.string.share_hotel_selection_title
        is AnalyzeDocumentUiState.FlightConfirm -> R.string.documents_analyze_confirm_flight_title
        is AnalyzeDocumentUiState.HotelConfirm -> R.string.documents_analyze_confirm_hotel_title
        else -> R.string.documents_analyze_title
    }

    // FlightLegSelection and HotelDestinationSelection use an inner LazyColumn which must not be
    // nested inside a verticalScroll container.
    val scrollState = rememberScrollState()
    val useScroll = analyzeState !is AnalyzeDocumentUiState.FlightLegSelection &&
        analyzeState !is AnalyzeDocumentUiState.HotelDestinationSelection

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .then(if (useScroll) Modifier.verticalScroll(scrollState) else Modifier),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (analyzeState) {
                    is AnalyzeDocumentUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text(
                            text = stringResource(R.string.documents_analyze_analyzing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is AnalyzeDocumentUiState.Downloading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text(
                            text = stringResource(R.string.documents_analyze_downloading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (analyzeState.bytesDownloaded > 0) {
                            Text(
                                text = stringResource(
                                    R.string.documents_analyze_downloaded_bytes,
                                    formatBytes(analyzeState.bytesDownloaded),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    is AnalyzeDocumentUiState.Unavailable -> {
                        Text(
                            text = stringResource(R.string.documents_analyze_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is AnalyzeDocumentUiState.Error -> {
                        Column(
                            modifier = Modifier.semantics(mergeDescendants = true) {},
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.documents_analyze_error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            analyzeState.message?.let { errorDetail ->
                                Text(
                                    text = errorDetail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.Result -> {
                        val extraction = analyzeState.extractionResult
                        // Summary section
                        Text(
                            text = stringResource(R.string.documents_analyze_summary_label),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = extraction.summary.ifBlank {
                                stringResource(R.string.documents_analyze_no_summary)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        // Proposed changes section
                        val hasChanges = extraction.hasProposedChanges()
                        if (hasChanges) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.documents_analyze_proposed_changes_label),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            extraction.flightInfo?.let { flight ->
                                AnalyzeInfoSection(
                                    label = stringResource(R.string.documents_analyze_flight_info_label),
                                    info = buildFlightInfoText(
                                        flight = flight,
                                        formattedFrom = flight.departurePlace?.let {
                                            stringResource(R.string.documents_analyze_from, it)
                                        },
                                        formattedTo = flight.arrivalPlace?.let {
                                            stringResource(R.string.documents_analyze_to, it)
                                        },
                                        formattedRef = flight.bookingReference?.let {
                                            stringResource(R.string.documents_analyze_ref, it)
                                        },
                                    ),
                                )
                            }
                            extraction.hotelInfo?.let { hotel ->
                                AnalyzeInfoSection(
                                    label = stringResource(R.string.documents_analyze_hotel_info_label),
                                    info = buildHotelInfoText(
                                        hotel = hotel,
                                        formattedRef = hotel.bookingReference?.let {
                                            stringResource(R.string.documents_analyze_ref, it)
                                        },
                                    ),
                                )
                            }
                            extraction.relevantTripInfo?.let { tripInfo ->
                                AnalyzeInfoSection(
                                    label = stringResource(R.string.documents_analyze_trip_info_label),
                                    info = tripInfo,
                                )
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.FlightConfirm -> {
                        AnalyzeFlightInfoSummary(flightInfo = analyzeState.flightInfo)
                        Text(
                            text = stringResource(R.string.documents_analyze_confirm_flight_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.AirplanemodeActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Column {
                                val legLabel = listOfNotNull(
                                    analyzeState.matchedLeg.company,
                                    analyzeState.matchedLeg.flightNumber,
                                ).joinToString(" ")
                                    .ifBlank { stringResource(R.string.share_unnamed_flight_leg) }
                                Text(
                                    text = legLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (!analyzeState.matchedLeg.reservationConfirmationNumber.isNullOrBlank()) {
                                    Text(
                                        text = stringResource(
                                            R.string.documents_analyze_ref,
                                            analyzeState.matchedLeg.reservationConfirmationNumber,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!analyzeState.matchedLeg.stopName.isNullOrBlank()) {
                                    Text(
                                        text = stringResource(
                                            R.string.documents_analyze_to,
                                            analyzeState.matchedLeg.stopName,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.HotelConfirm -> {
                        AnalyzeHotelInfoSummary(hotelInfo = analyzeState.hotelInfo)
                        if (analyzeState.existingHotel != null) {
                            Text(
                                text = stringResource(
                                    R.string.documents_analyze_confirm_hotel_message,
                                    analyzeState.destination.name,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.documents_analyze_confirm_hotel_new_message,
                                    analyzeState.destination.name,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Column {
                                Text(
                                    text = analyzeState.destination.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                val arrival = analyzeState.destination.arrivalDateTime?.toLocalDate()
                                val departure = analyzeState.destination.departureDateTime?.toLocalDate()
                                if (arrival != null || departure != null) {
                                    val dateRange = when {
                                        arrival != null && departure != null ->
                                            "${arrival.format(dateFormatter)} – ${departure.format(dateFormatter)}"
                                        arrival != null -> arrival.format(dateFormatter)
                                        else -> requireNotNull(departure).format(dateFormatter)
                                    }
                                    Text(
                                        text = dateRange,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                analyzeState.existingHotel?.name?.ifBlank { null }?.let { hotelName ->
                                    Text(
                                        text = hotelName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.FlightLegSelection -> {
                        AnalyzeFlightInfoSummary(flightInfo = analyzeState.flightInfo)
                        Text(
                            text = stringResource(R.string.share_flight_selection_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(analyzeState.candidates) { leg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onFlightLegSelected(leg) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AirplanemodeActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(end = 12.dp),
                                    )
                                    Column {
                                        val label = listOfNotNull(leg.company, leg.flightNumber)
                                            .joinToString(" ")
                                            .ifBlank { stringResource(R.string.share_unnamed_flight_leg) }
                                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                                        if (!leg.reservationConfirmationNumber.isNullOrBlank()) {
                                            Text(
                                                text = stringResource(
                                                    R.string.documents_analyze_ref,
                                                    leg.reservationConfirmationNumber,
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.HotelDestinationSelection -> {
                        AnalyzeHotelInfoSummary(hotelInfo = analyzeState.hotelInfo)
                        Text(
                            text = stringResource(R.string.share_hotel_selection_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(analyzeState.candidates) { destination ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onHotelDestinationSelected(destination) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .padding(end = 12.dp),
                                    )
                                    Column {
                                        Text(
                                            text = destination.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        val arrival = destination.arrivalDateTime?.toLocalDate()
                                        val departure = destination.departureDateTime?.toLocalDate()
                                        if (arrival != null || departure != null) {
                                            val dateRange = when {
                                                arrival != null && departure != null ->
                                                    "${arrival.format(dateFormatter)} – ${departure.format(dateFormatter)}"
                                                arrival != null -> arrival.format(dateFormatter)
                                                else -> requireNotNull(departure).format(dateFormatter)
                                            }
                                            Text(
                                                text = dateRange,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            when {
                analyzeState is AnalyzeDocumentUiState.Result &&
                    analyzeState.extractionResult.hasProposedChanges() -> {
                    TextButton(onClick = onApplyChanges) {
                        Text(stringResource(R.string.documents_analyze_apply_changes))
                    }
                }
                analyzeState is AnalyzeDocumentUiState.FlightConfirm -> {
                    TextButton(onClick = onFlightConfirmed) {
                        Text(stringResource(R.string.documents_analyze_confirm_apply))
                    }
                }
                analyzeState is AnalyzeDocumentUiState.HotelConfirm -> {
                    TextButton(onClick = onHotelConfirmed) {
                        Text(stringResource(R.string.documents_analyze_confirm_apply))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                val labelRes = when (analyzeState) {
                    is AnalyzeDocumentUiState.FlightLegSelection,
                    is AnalyzeDocumentUiState.HotelDestinationSelection,
                    -> R.string.share_skip
                    else -> R.string.dialog_cancel
                }
                Text(stringResource(labelRes))
            }
        },
    )
}

@Composable
private fun AnalyzeFlightInfoSummary(flightInfo: FlightInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.AirplanemodeActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp),
        )
        Column {
            val flightLabel = listOfNotNull(flightInfo.airline, flightInfo.flightNumber)
                .joinToString(" ")
                .ifBlank { stringResource(R.string.share_unknown_flight) }
            Text(text = flightLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (!flightInfo.bookingReference.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.documents_analyze_ref, flightInfo.bookingReference),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AnalyzeHotelInfoSummary(hotelInfo: HotelInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Hotel,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp),
        )
        Column {
            val hotelLabel = hotelInfo.name?.ifBlank { null }
                ?: stringResource(R.string.share_unknown_hotel)
            Text(
                text = hotelLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!hotelInfo.bookingReference.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.documents_analyze_ref, hotelInfo.bookingReference),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AnalyzeInfoSection(label: String, info: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = info,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun buildFlightInfoText(
    flight: FlightInfo,
    formattedFrom: String?,
    formattedTo: String?,
    formattedRef: String?,
): String =
    listOfNotNull(
        flight.airline,
        flight.flightNumber,
        formattedFrom,
        formattedTo,
        formattedRef,
    ).joinToString(" · ")

private fun buildHotelInfoText(hotel: HotelInfo, formattedRef: String?): String =
    listOfNotNull(
        hotel.name,
        hotel.address,
        formattedRef,
    ).joinToString(" · ")

private fun DocumentExtractionResult.hasProposedChanges(): Boolean =
    flightInfo != null || hotelInfo != null || relevantTripInfo != null

/**
 * Formats [bytes] as a human-readable file size string (B / KB / MB).
 * Used to display Gemini Nano model download progress in the analysis dialog.
 */
private fun formatBytes(bytes: Long): String = when {
    bytes < BYTES_PER_KB -> "$bytes B"
    bytes < BYTES_PER_MB -> "${bytes / BYTES_PER_KB} KB"
    else -> "%.1f MB".format(bytes.toFloat() / BYTES_PER_MB.toFloat())
}

private const val BYTES_PER_KB = 1_024L
private const val BYTES_PER_MB = 1_024L * 1_024L

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
        TripDocument(id = 1, tripId = 1, name = "passport_scan.pdf", uri = "", mimeType = "application/pdf"),
        TripDocument(id = 2, tripId = 1, name = "travel_insurance.pdf", uri = "", mimeType = "application/pdf"),
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

