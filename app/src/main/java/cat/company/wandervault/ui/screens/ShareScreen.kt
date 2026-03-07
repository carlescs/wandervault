package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import org.koin.androidx.compose.koinViewModel

/**
 * Entry point for the Share / "Save to Trip" screen.
 *
 * Shown when the app receives an ACTION_SEND intent.
 */
@Composable
fun ShareScreen(
    shareIntent: android.content.Intent,
    onNavigateUp: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ShareViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(shareIntent) {
        viewModel.handleIntent(shareIntent)
    }

    LaunchedEffect(uiState) {
        if (uiState is ShareUiState.Saved) onSaved()
    }

    ShareContent(
        uiState = uiState,
        onNavigateUp = {
            viewModel.cancel()
            onNavigateUp()
        },
        onTripSelected = viewModel::selectTrip,
        onFolderChanged = viewModel::setFolder,
        onSave = viewModel::saveDocument,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShareContent(
    uiState: ShareUiState,
    onNavigateUp: () -> Unit,
    onTripSelected: (Int) -> Unit,
    onFolderChanged: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.share_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.share_navigate_up),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is ShareUiState.Processing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.share_processing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            is ShareUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = uiState.message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            is ShareUiState.Saved -> {
                // Navigation handled by LaunchedEffect in the entry-point composable.
            }

            is ShareUiState.Ready -> {
                ShareReadyContent(
                    uiState = uiState,
                    innerPadding = innerPadding,
                    onTripSelected = onTripSelected,
                    onFolderChanged = onFolderChanged,
                    onSave = onSave,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareReadyContent(
    uiState: ShareUiState.Ready,
    innerPadding: androidx.compose.foundation.layout.PaddingValues,
    onTripSelected: (Int) -> Unit,
    onFolderChanged: (String) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // File name / title
        Text(
            text = stringResource(R.string.share_file_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = uiState.fileName,
            style = MaterialTheme.typography.bodyLarge,
        )

        // Extracted text preview
        if (!uiState.extractedText.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.share_extracted_text_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = uiState.extractedText.take(500) +
                    if (uiState.extractedText.length > 500) "…" else "",
                style = MaterialTheme.typography.bodySmall,
            )
        }

        // Trip selector
        Text(
            text = stringResource(R.string.share_trip_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (uiState.trips.isEmpty()) {
            Text(
                text = stringResource(R.string.share_no_trips),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        } else {
            var expanded by remember { mutableStateOf(false) }
            val selectedTrip = uiState.trips.firstOrNull { it.id == uiState.selectedTripId }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selectedTrip?.title ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.share_trip_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    uiState.trips.forEach { trip ->
                        DropdownMenuItem(
                            text = { Text(trip.title) },
                            onClick = {
                                onTripSelected(trip.id)
                                expanded = false
                            },
                        )
                    }
                }
            }
        }

        // Folder input
        OutlinedTextField(
            value = uiState.folder,
            onValueChange = onFolderChanged,
            label = { Text(stringResource(R.string.share_folder_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSave,
            enabled = uiState.selectedTripId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.share_save))
        }
    }
}
