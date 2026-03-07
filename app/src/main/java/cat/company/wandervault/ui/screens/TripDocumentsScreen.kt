package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.TripDocument
import cat.company.wandervault.domain.model.TripDocumentFolder
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

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
    TripDocumentsContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onOpenFolder = viewModel::openFolder,
        onNavigateUp = viewModel::navigateUp,
        onCreateFolder = viewModel::createFolder,
        onRenameFolder = viewModel::renameFolder,
        onDeleteFolder = viewModel::removeFolder,
        onRenameDocument = viewModel::renameDocument,
        onDeleteDocument = viewModel::removeDocument,
        onErrorDismiss = viewModel::clearError,
    )
}

/**
 * Stateless presentation of the Documents tab content.
 *
 * Supports folder navigation, create/rename/delete dialogs, and document rename/delete.
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
    onDeleteDocument: (TripDocument) -> Unit = {},
    onErrorDismiss: () -> Unit = {},
) {
    var showCreateFolderDialog by rememberSaveable { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<TripDocumentFolder?>(null) }
    var folderToDelete by remember { mutableStateOf<TripDocumentFolder?>(null) }
    var documentToRename by remember { mutableStateOf<TripDocument?>(null) }
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
                                text = stringResource(R.string.documents_empty_subtitle),
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
                                    onRename = { documentToRename = document },
                                    onDelete = { documentToDelete = document },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }

                // FAB: create a new folder (visible at all levels)
                FloatingActionButton(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(innerPadding)
                        .padding(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.documents_add_folder),
                    )
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
        Icon(
            imageVector = Icons.Default.Description,
            contentDescription = stringResource(R.string.documents_document_content_desc, document.name),
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(8.dp),
        )
        Text(
            text = document.name,
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
        TripDocument(id = 1, folderId = 1, name = "outbound.pdf", uri = "", mimeType = "application/pdf"),
        TripDocument(id = 2, folderId = 1, name = "return.pdf", uri = "", mimeType = "application/pdf"),
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
