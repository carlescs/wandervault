package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate

/**
 * Archive screen – shows the trips the user has archived.
 *
 * Displays an empty-state message when no archived trips exist, or a
 * scrollable list of [ArchivedTripCard]s otherwise.
 */
@Composable
fun ArchiveScreen(
    modifier: Modifier = Modifier,
    viewModel: ArchiveViewModel = koinViewModel(),
    onTripClick: (Int) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val unarchivedMessage = stringResource(R.string.trip_unarchived_snackbar)
    val undoLabel = stringResource(R.string.undo)
    LaunchedEffect(Unit) {
        viewModel.unarchiveUndoEvents.collect { trip ->
            val result = snackbarHostState.showSnackbar(
                message = unarchivedMessage,
                actionLabel = undoLabel,
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.onUndoUnarchive(trip)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        ArchiveContent(
            uiState = uiState,
            onTripClick = onTripClick,
            onUnarchiveClick = viewModel::onUnarchiveTrip,
            modifier = Modifier.fillMaxSize(),
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

/**
 * Stateless presentation of the Archive screen.
 *
 * Accepts an [ArchiveUiState] snapshot and event callbacks so it can be reused
 * in `@Preview` functions without a real [ArchiveViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArchiveContent(
    uiState: ArchiveUiState,
    onTripClick: (Int) -> Unit = {},
    onUnarchiveClick: (Trip) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.showEmptyState) {
        ArchiveEmptyState(modifier = modifier)
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    ScreenHeader(title = stringResource(R.string.archive_title))
                }
                items(uiState.trips, key = { it.id }) { trip ->
                    val currentUnarchiveAction by rememberUpdatedState(newValue = { onUnarchiveClick(trip) })
                    SwipeToRevealBox(
                        icon = Icons.Default.Unarchive,
                        iconContentDescription = stringResource(R.string.unarchive_trip_content_desc),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                        onAction = { currentUnarchiveAction() },
                        modifier = Modifier.animateItem(),
                    ) {
                        ArchivedTripCard(
                            trip = trip,
                            onCardClick = { onTripClick(trip.id) },
                            onUnarchiveClick = { onUnarchiveClick(trip) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedTripCard(
    trip: Trip,
    onCardClick: () -> Unit = {},
    onUnarchiveClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    TripCoverCard(trip = trip, onClick = onCardClick, modifier = modifier, desaturated = true) {
        IconButton(onClick = onUnarchiveClick) {
            Icon(
                imageVector = Icons.Default.Unarchive,
                contentDescription = stringResource(R.string.unarchive_trip_content_desc),
            )
        }
    }
}

@Composable
private fun ArchiveEmptyState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.Inventory2,
        title = stringResource(R.string.archive_empty_title),
        message = stringResource(R.string.archive_empty),
        modifier = modifier,
    )
}

@Preview(showBackground = true)
@Composable
private fun ArchiveScreenEmptyPreview() {
    WanderVaultTheme {
        ArchiveContent(
            uiState = ArchiveUiState(isLoading = false),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ArchiveScreenWithTripsPreview() {
    val sampleTrips = listOf(
        Trip(1, "Paris Getaway", isArchived = true, startDate = LocalDate.of(2023, 6, 1), endDate = LocalDate.of(2023, 6, 10)),
        Trip(2, "Tokyo Adventure", isArchived = true),
    )
    WanderVaultTheme {
        ArchiveContent(
            uiState = ArchiveUiState(trips = sampleTrips, isLoading = false),
        )
    }
}
