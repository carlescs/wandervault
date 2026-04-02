package cat.company.wandervault.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.User
import cat.company.wandervault.ui.theme.WanderVaultTheme
import coil.compose.AsyncImage
import org.koin.androidx.compose.koinViewModel

/**
 * Profile screen showing sign-in state and account actions.
 *
 * @param onNavigateToSettings Called when the user taps the settings icon.
 * @param modifier Optional [Modifier].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit = {},
    onNavigateToJoinTrip: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onSignInResult(result.data)
    }

    LaunchedEffect(viewModel) {
        viewModel.signInIntentEvent.collect { intent ->
            signInLauncher.launch(intent)
        }
    }

    ProfileContent(
        uiState = uiState,
        onNavigateToSettings = onNavigateToSettings,
        onNavigateToJoinTrip = onNavigateToJoinTrip,
        onSignInClick = viewModel::onSignInClick,
        onSignOutClick = viewModel::onSignOutClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProfileContent(
    uiState: ProfileUiState,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToJoinTrip: () -> Unit = {},
    onSignInClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
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
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else if (uiState.user != null) {
                SignedInContent(
                    user = uiState.user,
                    onSignOutClick = onSignOutClick,
                    onJoinTripClick = onNavigateToJoinTrip,
                )
            } else {
                SignedOutContent(
                    error = uiState.error,
                    onSignInClick = onSignInClick,
                )
            }
        }
    }
}

@Composable
private fun SignedInContent(
    user: User,
    onSignOutClick: () -> Unit,
    onJoinTripClick: () -> Unit,
) {
    if (user.photoUrl != null) {
        AsyncImage(
            model = user.photoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape),
        )
    } else {
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = user.displayName ?: stringResource(R.string.profile_unknown_name),
        style = MaterialTheme.typography.headlineSmall,
    )
    if (user.email != null) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = user.email,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Spacer(modifier = Modifier.height(32.dp))
    Button(onClick = onJoinTripClick) {
        Text(stringResource(R.string.profile_join_trip))
    }
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedButton(onClick = onSignOutClick) {
        Text(stringResource(R.string.profile_sign_out))
    }
}

@Composable
private fun SignedOutContent(
    error: String?,
    onSignInClick: () -> Unit,
) {
    Icon(
        imageVector = Icons.Default.AccountCircle,
        contentDescription = null,
        modifier = Modifier.size(80.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = stringResource(R.string.profile_subtitle),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(modifier = Modifier.height(24.dp))
    Button(onClick = onSignInClick) {
        Text(stringResource(R.string.profile_sign_in_with_google))
    }
    if (error != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenSignedOutPreview() {
    WanderVaultTheme {
        ProfileContent(uiState = ProfileUiState())
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfileScreenSignedInPreview() {
    WanderVaultTheme {
        ProfileContent(
            uiState = ProfileUiState(
                user = User(
                    uid = "uid123",
                    displayName = "Jane Doe",
                    email = "jane@example.com",
                    photoUrl = null,
                ),
            ),
            onNavigateToJoinTrip = {},
        )
    }
}
