package cat.company.wandervault.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.ui.theme.WanderVaultTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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

    ArchiveContent(
        uiState = uiState,
        onTripClick = onTripClick,
        onUnarchiveClick = viewModel::onUnarchiveTrip,
        modifier = modifier,
    )
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.trips, key = { it.id }) { trip ->
                    val swipeState = rememberSwipeToDismissBoxState()
                    SwipeToDismissBox(
                        state = swipeState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            SwipeToUnarchiveBackground(
                                swipeState = swipeState,
                                onUnarchiveClick = { onUnarchiveClick(trip) },
                            )
                        },
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
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Card(modifier = modifier.fillMaxWidth(), onClick = onCardClick) {
        if (trip.imageUri != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(Uri.parse(trip.imageUri))
                    .crossfade(true)
                    .build(),
                contentDescription = stringResource(R.string.trip_image_content_desc),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
            )
        }
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                if (trip.startDate != null && trip.endDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${trip.startDate.format(formatter)} – ${trip.endDate.format(formatter)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(onClick = onUnarchiveClick) {
                Icon(
                    imageVector = Icons.Default.Unarchive,
                    contentDescription = stringResource(R.string.unarchive_trip_content_desc),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToUnarchiveBackground(swipeState: SwipeToDismissBoxState, onUnarchiveClick: () -> Unit) {
    val isRevealed = swipeState.currentValue == SwipeToDismissBoxValue.EndToStart
    val isActive = swipeState.targetValue == SwipeToDismissBoxValue.EndToStart
    val isSwiping = swipeState.dismissDirection == SwipeToDismissBoxValue.EndToStart
    val containerColor by animateColorAsState(
        when {
            isActive -> MaterialTheme.colorScheme.primaryContainer
            isSwiping -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = SWIPE_HINT_BG_ALPHA)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0f)
        },
        label = "unarchive_swipe_bg",
    )
    val iconTint by animateColorAsState(
        if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.primary.copy(alpha = SWIPE_HINT_ICON_ALPHA),
        label = "unarchive_icon_tint",
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CardDefaults.shape)
            .background(containerColor)
            .clickable(
                enabled = isRevealed,
                role = Role.Button,
                onClickLabel = stringResource(R.string.unarchive_trip_content_desc),
                onClick = onUnarchiveClick,
            )
            .padding(end = 20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        AnimatedVisibility(
            visible = isSwiping || isActive,
            enter = fadeIn() + scaleIn(initialScale = 0.75f),
            exit = fadeOut() + scaleOut(targetScale = 0.75f),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Unarchive,
                    contentDescription = null,
                    tint = iconTint,
                )
                AnimatedVisibility(visible = isActive) {
                    Text(
                        text = stringResource(R.string.unarchive_trip_content_desc),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.archive_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.archive_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
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
