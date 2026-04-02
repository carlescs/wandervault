package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel

/**
 * Screen that allows a user to join a shared trip by entering a 6-character invite code.
 *
 * @param onNavigateUp Called when the user taps the back button.
 * @param onTripJoined Called with the local trip ID when the trip has been successfully joined.
 */
@Composable
fun JoinTripScreen(
    onNavigateUp: () -> Unit,
    onTripJoined: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JoinTripViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.joinedTripId) {
        uiState.joinedTripId?.let { onTripJoined(it) }
    }

    JoinTripContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onCodeChanged = viewModel::onInviteCodeChanged,
        onJoinClick = viewModel::onJoinClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JoinTripContent(
    uiState: JoinTripUiState,
    onNavigateUp: () -> Unit = {},
    onCodeChanged: (String) -> Unit = {},
    onJoinClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.join_trip_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.join_trip_navigate_up),
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.join_trip_description),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = uiState.inviteCode,
                onValueChange = onCodeChanged,
                label = { Text(stringResource(R.string.join_trip_code_label)) },
                placeholder = { Text(stringResource(R.string.join_trip_code_placeholder)) },
                singleLine = true,
                isError = uiState.error != null,
                supportingText = {
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = onJoinClick,
                    enabled = uiState.isCodeValid,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.join_trip_button))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JoinTripScreenPreview() {
    WanderVaultTheme {
        JoinTripContent(uiState = JoinTripUiState())
    }
}
