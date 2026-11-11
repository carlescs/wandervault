package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    if (uiState.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (uiState.showEmptyState) {
        FavoritesEmptyState(modifier = modifier)
    } else {
        Box(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    ScreenHeader(title = stringResource(R.string.favorites_title))
                }
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
    TripCoverCard(trip = trip, onClick = onCardClick, modifier = modifier) {
        IconToggleButton(
            checked = true,
            onCheckedChange = { onUnfavoriteClick() },
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = stringResource(R.string.trip_remove_favorite),
            )
        }
    }
}

@Composable
private fun FavoritesEmptyState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.FavoriteBorder,
        title = stringResource(R.string.favorites_empty_title),
        message = stringResource(R.string.favorites_empty),
        modifier = modifier,
    )
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
