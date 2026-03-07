package cat.company.wandervault.ui.screens

import android.net.Uri
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
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
 * Favorites screen – shows the trips the user has marked as favorites.
 *
 * Displays an empty-state message when no favorites exist yet, or a
 * scrollable list of [FavoriteTripCard]s otherwise.
 */
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = koinViewModel(),
    onTripClick: (Int) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    FavoritesContent(
        uiState = uiState,
        onTripClick = onTripClick,
        onUnfavoriteClick = viewModel::onToggleFavorite,
        modifier = modifier,
    )
}

/**
 * Stateless presentation of the Favorites screen.
 *
 * Accepts a [FavoritesUiState] snapshot and event callbacks so it can be reused
 * in `@Preview` functions without a real [FavoritesViewModel].
 */
@Composable
internal fun FavoritesContent(
    uiState: FavoritesUiState,
    onTripClick: (Int) -> Unit = {},
    onUnfavoriteClick: (Trip) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!uiState.isLoading && uiState.trips.isEmpty()) {
        FavoritesEmptyState(modifier = modifier)
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.trips, key = { it.id }) { trip ->
                    FavoriteTripCard(
                        trip = trip,
                        onCardClick = { onTripClick(trip.id) },
                        onUnfavoriteClick = { onUnfavoriteClick(trip) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteTripCard(
    trip: Trip,
    onCardClick: () -> Unit = {},
    onUnfavoriteClick: () -> Unit = {},
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
            IconButton(onClick = onUnfavoriteClick) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.trip_remove_favorite),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun FavoritesEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.favorites_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.favorites_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FavoritesScreenEmptyPreview() {
    WanderVaultTheme {
        FavoritesContent(
            uiState = FavoritesUiState(isLoading = false),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FavoritesScreenWithTripsPreview() {
    val sampleTrips = listOf(
        Trip(1, "Paris Getaway", isFavorite = true, startDate = LocalDate.of(2024, 6, 1), endDate = LocalDate.of(2024, 6, 10)),
        Trip(2, "Tokyo Adventure", isFavorite = true),
    )
    WanderVaultTheme {
        FavoritesContent(
            uiState = FavoritesUiState(trips = sampleTrips, isLoading = false),
        )
    }
}
