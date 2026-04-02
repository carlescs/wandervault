package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Screen that lets the trip owner share a trip, generate invite codes, and manage collaborators.
 *
 * @param tripId The ID of the trip to share.
 * @param onNavigateUp Called when the user taps the back button.
 */
@Composable
fun ShareTripScreen(
    tripId: Int,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShareTripViewModel = koinViewModel(
        key = "ShareTripViewModel:$tripId",
        parameters = { parametersOf(tripId) },
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShareTripContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onShareToggle = viewModel::onShareToggle,
        onGenerateInviteClick = viewModel::onGenerateInviteClick,
        onRemoveCollaboratorClick = viewModel::onRemoveCollaboratorClick,
        onSyncClick = viewModel::onSyncClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareTripContent(
    uiState: ShareTripUiState,
    onNavigateUp: () -> Unit = {},
    onShareToggle: () -> Unit = {},
    onGenerateInviteClick: () -> Unit = {},
    onRemoveCollaboratorClick: (String) -> Unit = {},
    onSyncClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_trip_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.share_trip_navigate_up),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (uiState) {
                is ShareTripUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is ShareTripUiState.Error -> {
                    Text(
                        text = uiState.message,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                is ShareTripUiState.Success -> {
                    ShareTripSuccessContent(
                        state = uiState,
                        onShareToggle = onShareToggle,
                        onGenerateInviteClick = onGenerateInviteClick,
                        onRemoveCollaboratorClick = onRemoveCollaboratorClick,
                        onSyncClick = onSyncClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun ShareTripSuccessContent(
    state: ShareTripUiState.Success,
    onShareToggle: () -> Unit,
    onGenerateInviteClick: () -> Unit,
    onRemoveCollaboratorClick: (String) -> Unit,
    onSyncClick: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.tripTitle,
                style = MaterialTheme.typography.titleLarge,
            )
        }

        // ── Enable/disable sharing toggle ──
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        stringResource(R.string.share_trip_enable_label),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    if (state.isShared) {
                        Text(
                            text = stringResource(R.string.share_trip_share_id, state.shareId ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (state.isSyncing) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp))
                } else {
                    Switch(
                        checked = state.isShared,
                        onCheckedChange = { onShareToggle() },
                        enabled = state.isOwner || !state.isShared,
                    )
                }
            }
        }

        if (state.isShared) {
            item { HorizontalDivider() }

            // ── Invite code section ──
            item {
                Text(
                    stringResource(R.string.share_trip_invite_section),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            item {
                if (state.inviteCode != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = state.inviteCode,
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(state.inviteCode))
                            },
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = stringResource(R.string.share_trip_copy_code),
                            )
                        }
                    }
                }
                if (state.isGeneratingInvite) {
                    CircularProgressIndicator()
                } else if (state.isOwner) {
                    Button(onClick = onGenerateInviteClick) {
                        Text(stringResource(R.string.share_trip_generate_invite))
                    }
                }
            }

            item { HorizontalDivider() }

            // ── Sync section ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        stringResource(R.string.share_trip_sync_label),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    OutlinedButton(
                        onClick = onSyncClick,
                        enabled = !state.isSyncing,
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Text(stringResource(R.string.share_trip_sync_button))
                    }
                }
            }

            // ── Collaborators section ──
            if (state.collaboratorIds.isNotEmpty()) {
                item { HorizontalDivider() }
                item {
                    Text(
                        stringResource(R.string.share_trip_collaborators_section),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                items(state.collaboratorIds) { uid ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = uid,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        if (state.isOwner) {
                            IconButton(onClick = { onRemoveCollaboratorClick(uid) }) {
                                Icon(
                                    Icons.Default.PersonRemove,
                                    contentDescription = stringResource(
                                        R.string.share_trip_remove_collaborator,
                                    ),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.error != null) {
            item {
                Text(
                    text = state.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareTripScreenSharedPreview() {
    WanderVaultTheme {
        ShareTripContent(
            uiState = ShareTripUiState.Success(
                tripTitle = "Summer in Paris",
                shareId = "abc-123",
                ownerId = "uid1",
                currentUserUid = "uid1",
                collaboratorIds = listOf("uid2", "uid3"),
                inviteCode = "ABC123",
            ),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareTripScreenUnsharedPreview() {
    WanderVaultTheme {
        ShareTripContent(
            uiState = ShareTripUiState.Success(
                tripTitle = "Summer in Paris",
                shareId = null,
                ownerId = null,
                currentUserUid = "uid1",
                collaboratorIds = emptyList(),
            ),
        )
    }
}
