package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.ui.theme.WanderVaultTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Stateful entry point for the Map tab.
 *
 * Shares the [ItineraryViewModel] instance (same Koin scope as [ItineraryTabContent]) so no
 * extra data-fetching is needed.
 *
 * @param tripId The ID of the trip whose map is shown.
 * @param innerPadding Scaffold inner padding forwarded from the parent [Scaffold].
 */
@Composable
internal fun MapTabContent(
    tripId: Int,
    innerPadding: PaddingValues,
    viewModel: ItineraryViewModel = koinViewModel(parameters = { parametersOf(tripId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MapContent(
        destinations = uiState.destinations,
        isLoading = uiState.isLoading,
        innerPadding = innerPadding,
    )
}

/**
 * Stateless map content composable.
 *
 * Renders a Google Map with a pin for each destination that has [Destination.latitude] and
 * [Destination.longitude] set, connected by a polyline in itinerary order.
 *
 * Shows appropriate empty/hint states when destinations are missing or have no coordinates.
 */
@Composable
internal fun MapContent(
    destinations: List<Destination>,
    isLoading: Boolean,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val mappableDestinations = destinations.filter { it.latitude != null && it.longitude != null }

    when {
        isLoading -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        destinations.isEmpty() -> {
            MapEmptyState(
                title = stringResource(R.string.map_empty_title),
                subtitle = stringResource(R.string.map_empty_subtitle),
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }

        mappableDestinations.isEmpty() -> {
            MapEmptyState(
                title = stringResource(R.string.map_no_coordinates_title),
                subtitle = stringResource(R.string.map_no_coordinates_subtitle),
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }

        else -> {
            val cameraPositionState = rememberCameraPositionState()
            val polylineColor: Color = MaterialTheme.colorScheme.primary
            // Pre-compute stop labels outside the GoogleMapComposable scope.
            val stopLabels = mappableDestinations.mapIndexed { index, _ ->
                stringResource(R.string.map_destination_stop, index + 1)
            }
            val points = mappableDestinations.map { LatLng(it.latitude!!, it.longitude!!) }

            LaunchedEffect(mappableDestinations) {
                if (points.size == 1) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(points.first(), 12f),
                    )
                } else {
                    val boundsBuilder = LatLngBounds.builder()
                    points.forEach { boundsBuilder.include(it) }
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 64),
                    )
                }
            }

            GoogleMap(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                cameraPositionState = cameraPositionState,
            ) {
                points.forEachIndexed { index, latLng ->
                    Marker(
                        state = MarkerState(position = latLng),
                        title = mappableDestinations[index].name,
                        snippet = stopLabels[index],
                    )
                }

                if (points.size > 1) {
                    Polyline(
                        points = points,
                        color = polylineColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun MapEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun MapEmptyPreview() {
    WanderVaultTheme {
        MapContent(
            destinations = emptyList(),
            isLoading = false,
            innerPadding = PaddingValues(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MapNoCoordinatesPreview() {
    WanderVaultTheme {
        MapContent(
            destinations = listOf(
                Destination(id = 1, tripId = 1, name = "London", position = 0),
                Destination(id = 2, tripId = 1, name = "Paris", position = 1),
            ),
            isLoading = false,
            innerPadding = PaddingValues(),
        )
    }
}
