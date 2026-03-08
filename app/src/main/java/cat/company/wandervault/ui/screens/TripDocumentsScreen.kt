package cat.company.wandervault.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.io.File

/**
 * Documents tab entry point for the Trip Detail screen.
 *
 * @param tripId The ID of the trip whose documents are shown.
 * @param innerPadding Padding values provided by the parent [Scaffold].
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
                val uri = Uri.parse(document.uri)
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
            } catch (e: ActivityNotFoundException) {
                android.widget.Toast.makeText(context, noAppMessage, android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: IllegalArgumentException) {
                android.widget.Toast.makeText(context, noAppMessage, android.widget.Toast.LENGTH_SHORT).show()
            }
        },
        onErrorDismiss = viewModel::clearError,
    )
}

/**
 * Stateless presentation of the Documents tab content.
 *
 * Supports folder navigation, create/rename/delete dialogs, document rename/delete,
 * file upload (via [onUploadFile]), and document open (via [onOpenDocument]).
 * [onErrorDismiss] is called when the user acknowledges a transient write error.
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
    onErrorDismiss: () -> Unit = {},
) {
    var isFabExpanded by rememberSaveable { mutableStateOf(false) }
    var showCreateFolderDialog by rememberSaveable { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<TripDocumentFolder?>(null) }
    var folderToDelete by remember { mutableStateOf<TripDocumentFolder?>(null) }
    var documentToRename by remember { mutableStateOf<TripDocument?>(null) }
    var documentToMove by remember { mutableStateOf<TripDocument?>(null) }
    var documentToDelete by remember { mutableStateOf<TripDocument?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState) {
            is TripDocumentsUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }

            is TripDocumentsUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    // Breadcrumb / back row when inside a sub-folder
                    if (uiState.currentFolder != null) {
                        // Drop the current folder from the stack to find the immediate parent.
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
                                text = uiState.currentFolder.name,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        HorizontalDivider()
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
                                    onClick = { onOpenFolder(folder) },
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
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }

                // Speed-dial FAB: expands to show "Upload file" and "New folder" actions.
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

    // ── Dialogs ──────────────────────────────────────────────────────────────

    if (showCreateFolderDialog) {
        NameInputDialog(
            title = stringResource(R.string.documents_new_folder_title),
            label = stringResource(R.string.documents_folder_name_label),
            initialName = "",
            onConfirm = { name ->
                onCreateFolder(name)
                showCreateFolderDialog = false
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
                onRenameFolder(folder, newName)
                folderToRename = null
            },
            onDismiss = { folderToRename = null },
        )
    }

    folderToDelete?.let { folder ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.documents_delete_folder_title),
            message = stringResource(R.string.documents_delete_folder_message, folder.name),
            onConfirm = {
                onDeleteFolder(folder)
                folderToDelete = null
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
                onRenameDocument(document, newName)
                documentToRename = null
            },
            onDismiss = { documentToRename = null },
        )
    }

    documentToMove?.let { document ->
        val allFolders = (uiState as? TripDocumentsUiState.Success)?.allFolders ?: emptyList()
        MoveFolderPickerDialog(
            allFolders = allFolders,
            onMove = { targetFolderId ->
                onMoveDocument(document, targetFolderId)
                documentToMove = null
            },
            onDismiss = { documentToMove = null },
        )
    }

    documentToDelete?.let { document ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.documents_delete_document_title),
            message = stringResource(R.string.documents_delete_document_message, document.name),
            onConfirm = {
                onDeleteDocument(document)
                documentToDelete = null
            },
            onDismiss = { documentToDelete = null },
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

@Composable
private fun FolderRow(
    folder: TripDocumentFolder,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        IconButton(onClick = onRename) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.documents_rename_action),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.documents_delete_action),
            )
        }
    }
}

@Composable
private fun DocumentRow(
    document: TripDocument,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onOpen) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = stringResource(R.string.documents_open_content_desc, document.name),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
        Text(
            text = document.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .clickable(onClick = onOpen),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onRename) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.documents_rename_action),
            )
        }
        IconButton(onClick = onMove) {
            Icon(
                imageVector = Icons.Default.DriveFileMove,
                contentDescription = stringResource(R.string.documents_move_action),
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.documents_delete_action),
            )
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
        // No confirm button — the dialog is dismissed automatically on item selection.
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
 * A visited-ID set guards against cycles in case of data corruption so the loop always terminates.
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

