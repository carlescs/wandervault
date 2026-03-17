package cat.company.wandervault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.DriveFolder
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Profile screen – displays the current user's account information and the
 * Google Drive integration section.
 *
 * The Drive section lets the user sign in / sign out and pick the Drive folder
 * that will receive copies of locally saved trip documents.
 *
 * @param onNavigateToSettings Called when the user taps the settings icon.
 * @param modifier Optional [Modifier].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ProfileContent(
        uiState = uiState,
        onNavigateToSettings = onNavigateToSettings,
        onSignIn = viewModel::onSignIn,
        onSignOut = viewModel::onSignOut,
        onOpenFolderPicker = viewModel::onOpenFolderPicker,
        onFolderSelected = viewModel::onFolderSelected,
        onClearFolderSelection = viewModel::onClearFolderSelection,
        onDismissError = viewModel::onDismissError,
        onDismissFolderPicker = viewModel::onDismissFolderPicker,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileContent(
    uiState: ProfileUiState,
    onNavigateToSettings: () -> Unit = {},
    onSignIn: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onOpenFolderPicker: () -> Unit = {},
    onFolderSelected: (DriveFolder) -> Unit = {},
    onClearFolderSelection: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onDismissFolderPicker: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title)) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.profile_settings_desc),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Account header ─────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(R.string.profile_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.profile_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            // ── Google Drive section ────────────────────────────────────────
            Text(
                text = stringResource(R.string.profile_drive_section),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()

            if (uiState.isSignedInToDrive) {
                DriveSignedInContent(
                    uiState = uiState,
                    onSignOut = onSignOut,
                    onOpenFolderPicker = onOpenFolderPicker,
                    onClearFolderSelection = onClearFolderSelection,
                )
            } else {
                DriveSignedOutContent(
                    isSigningIn = uiState.isSigningIn,
                    onSignIn = onSignIn,
                )
            }
        }
    }

    // ── Folder picker dialog ────────────────────────────────────────────────
    if (uiState.availableDriveFolders.isNotEmpty() || uiState.isLoadingFolders) {
        DriveFolderPickerDialog(
            folders = uiState.availableDriveFolders,
            isLoading = uiState.isLoadingFolders,
            onFolderSelected = onFolderSelected,
            onDismiss = onDismissFolderPicker,
        )
    }

    // ── Error dialog ────────────────────────────────────────────────────────
    if (uiState.driveError != null) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text(stringResource(R.string.profile_drive_error_title)) },
            text = { Text(uiState.driveError) },
            confirmButton = {
                TextButton(onClick = onDismissError) {
                    Text(stringResource(R.string.profile_drive_error_dismiss))
                }
            },
        )
    }
}

@Composable
private fun DriveSignedInContent(
    uiState: ProfileUiState,
    onSignOut: () -> Unit,
    onOpenFolderPicker: () -> Unit,
    onClearFolderSelection: () -> Unit,
) {
    // Sign-in status row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.profile_drive_signed_in),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(R.string.profile_drive_signed_in_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedButton(
            onClick = onSignOut,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.error,
            ),
        ) {
            Text(stringResource(R.string.profile_drive_sign_out))
        }
    }

    HorizontalDivider()

    // Folder selection row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenFolderPicker)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.profile_drive_folder_label),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = uiState.selectedDriveFolder?.name
                    ?: stringResource(R.string.profile_drive_folder_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (uiState.selectedDriveFolder != null) {
            IconButton(onClick = onClearFolderSelection) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(R.string.profile_drive_folder_clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DriveSignedOutContent(
    isSigningIn: Boolean,
    onSignIn: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.profile_drive_signed_out_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onSignIn,
            enabled = !isSigningIn,
        ) {
            if (isSigningIn) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(stringResource(R.string.profile_drive_sign_in))
        }
    }
}

@Composable
private fun DriveFolderPickerDialog(
    folders: List<DriveFolder>,
    isLoading: Boolean,
    onFolderSelected: (DriveFolder) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.profile_drive_folder_picker_title)) },
        text = {
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (folders.isEmpty()) {
                Text(stringResource(R.string.profile_drive_folder_picker_empty))
            } else {
                Column {
                    folders.forEach { folder ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFolderSelected(folder) }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.profile_drive_folder_picker_cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenSignedOutPreview() {
    WanderVaultTheme {
        ProfileContent(uiState = ProfileUiState(isSignedInToDrive = false))
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenSignedInNoFolderPreview() {
    WanderVaultTheme {
        ProfileContent(
            uiState = ProfileUiState(
                isSignedInToDrive = true,
                selectedDriveFolder = null,
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenSignedInWithFolderPreview() {
    WanderVaultTheme {
        ProfileContent(
            uiState = ProfileUiState(
                isSignedInToDrive = true,
                selectedDriveFolder = DriveFolder(id = "abc123", name = "WanderVault Docs"),
            ),
        )
    }
}
