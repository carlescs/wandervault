package cat.company.wandervault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Data Administration screen entry point.
 *
 * Provides backup and restore functionality for all app data (database + images).
 *
 * @param onNavigateUp Called when the user taps the back button.
 * @param modifier Optional [Modifier].
 */
@Composable
fun DataAdminScreen(
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DataAdminViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DataAdminContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onBackup = viewModel::onBackup,
        onRestore = viewModel::onRestore,
        onDismissMessage = viewModel::onDismissMessage,
        onRestartApp = viewModel::restartApp,
        modifier = modifier,
    )
}

/**
 * Stateless presentation of the Data Administration screen.
 *
 * Accepts a [DataAdminUiState] snapshot so it can be reused in `@Preview` without a real ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DataAdminContent(
    uiState: DataAdminUiState,
    onNavigateUp: () -> Unit,
    onBackup: (String) -> Unit,
    onRestore: (String) -> Unit,
    onDismissMessage: () -> Unit,
    onRestartApp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri ->
        uri?.toString()?.let { onBackup(it) }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.toString()?.let { onRestore(it) }
    }

    val backupErrorMessage = stringResource(R.string.data_admin_backup_error)
    val restoreErrorMessage = stringResource(R.string.data_admin_restore_error)
    LaunchedEffect(uiState) {
        when (uiState) {
            is DataAdminUiState.BackupError -> {
                snackbarHostState.showSnackbar(
                    uiState.message ?: backupErrorMessage,
                )
                onDismissMessage()
            }
            is DataAdminUiState.RestoreError -> {
                snackbarHostState.showSnackbar(
                    uiState.message ?: restoreErrorMessage,
                )
                onDismissMessage()
            }
            else -> Unit
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.data_admin_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.data_admin_navigate_up),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        val isAnyOperationInProgress =
            uiState is DataAdminUiState.BackupInProgress || uiState is DataAdminUiState.RestoreInProgress

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(
                text = stringResource(R.string.data_admin_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            // Backup section
            DataAdminActionCard(
                title = stringResource(R.string.data_admin_backup_title),
                description = stringResource(R.string.data_admin_backup_description),
                icon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(32.dp)) },
                action = {
                    when (uiState) {
                        is DataAdminUiState.BackupInProgress -> {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        }
                        is DataAdminUiState.BackupSuccess -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.data_admin_backup_success),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = onDismissMessage) {
                                    Text(stringResource(R.string.data_admin_dismiss))
                                }
                            }
                        }
                        else -> {
                            Button(
                                onClick = { backupLauncher.launch("wandervault-backup.zip") },
                                enabled = !isAnyOperationInProgress,
                            ) {
                                Text(stringResource(R.string.data_admin_backup_button))
                            }
                        }
                    }
                },
            )

            // Restore section
            DataAdminActionCard(
                title = stringResource(R.string.data_admin_restore_title),
                description = stringResource(R.string.data_admin_restore_description),
                icon = { Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(32.dp)) },
                action = {
                    when (uiState) {
                        is DataAdminUiState.RestoreInProgress -> {
                            CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        }
                        is DataAdminUiState.RestoreSuccess -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(R.string.data_admin_restore_success),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedButton(onClick = onRestartApp) {
                                    Text(stringResource(R.string.data_admin_restart))
                                }
                            }
                        }
                        else -> {
                            Button(
                                onClick = {
                                    restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                                },
                                enabled = !isAnyOperationInProgress,
                            ) {
                                Text(stringResource(R.string.data_admin_restore_button))
                            }
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun DataAdminActionCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    action: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                icon()
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
            action()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DataAdminContentIdlePreview() {
    WanderVaultTheme {
        DataAdminContent(
            uiState = DataAdminUiState.Idle,
            onNavigateUp = {},
            onBackup = {},
            onRestore = {},
            onDismissMessage = {},
            onRestartApp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DataAdminContentRestoreSuccessPreview() {
    WanderVaultTheme {
        DataAdminContent(
            uiState = DataAdminUiState.RestoreSuccess,
            onNavigateUp = {},
            onBackup = {},
            onRestore = {},
            onDismissMessage = {},
            onRestartApp = {},
        )
    }
}
