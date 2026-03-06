package cat.company.wandervault.ui.screens

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.ui.theme.WanderVaultTheme
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Tabs shown in the Trip Detail bottom navigation bar. */
private enum class TripDetailTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    DETAILS(R.string.trip_detail_tab_details, Icons.Default.Info),
    ITINERARY(R.string.trip_detail_tab_itinerary, Icons.Default.DateRange),
    CALENDAR(R.string.trip_detail_tab_calendar, Icons.Default.CalendarMonth),
}

/**
 * Trip Detail screen entry point.
 *
 * Fetches trip state via [TripDetailViewModel] and renders the detail content.
 *
 * @param tripId The ID of the trip to display.
 * @param onNavigateUp Called when the user taps the back/up button.
 * @param onNavigateToDestination Called when the user taps a destination in the itinerary.
 * @param onNavigateToTransport Called when the user taps the transport icon in the itinerary.
 * @param modifier Optional [Modifier].
 */
@Composable
fun TripDetailScreen(
    tripId: Int,
    onNavigateUp: () -> Unit,
    onNavigateToDestination: (Int) -> Unit = {},
    onNavigateToTransport: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TripDetailViewModel = koinViewModel(key = "TripDetailViewModel:$tripId", parameters = { parametersOf(tripId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TripDetailContent(
        uiState = uiState,
        tripId = tripId,
        onNavigateUp = onNavigateUp,
        onNavigateToDestination = onNavigateToDestination,
        onNavigateToTransport = onNavigateToTransport,
        modifier = modifier,
    )
}

/**
 * Stateless presentation of the Trip Detail screen.
 *
 * Accepts a [TripDetailUiState] snapshot so it can be reused in `@Preview` without a real ViewModel.
 *
 * @param tripId The ID of the trip – forwarded to [ItineraryTabContent] when the Itinerary tab is selected.
 * @param onNavigateToDestination Called when the user taps a destination in the itinerary tab.
 * @param onNavigateToTransport Called when the user taps the transport icon in the itinerary tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripDetailContent(
    uiState: TripDetailUiState,
    tripId: Int,
    onNavigateUp: () -> Unit,
    onNavigateToDestination: (Int) -> Unit = {},
    onNavigateToTransport: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(TripDetailTab.DETAILS) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (uiState is TripDetailUiState.Success) {
                            uiState.trip.title
                        } else {
                            stringResource(R.string.trip_detail_title)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.trip_detail_navigate_up),
                        )
                    }
                },
            )
        },
        bottomBar = {
            TripDetailBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        },
    ) { innerPadding ->
        when (selectedTab) {
            TripDetailTab.DETAILS -> TripDetailsTabContent(uiState = uiState, innerPadding = innerPadding)
            TripDetailTab.ITINERARY -> ItineraryTabContent(
                tripId = tripId,
                innerPadding = innerPadding,
                onDestinationClick = onNavigateToDestination,
                onTransportClick = onNavigateToTransport,
            )
            TripDetailTab.CALENDAR -> CalendarTabContent(tripId = tripId, innerPadding = innerPadding)
        }
    }
}

@Composable
private fun TripDetailBottomBar(
    selectedTab: TripDetailTab,
    onTabSelected: (TripDetailTab) -> Unit,
) {
    NavigationBar {
        TripDetailTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}

/**
 * Content for the Details tab of the Trip Detail screen.
 *
 * Shows a cover image (when available), trip title, formatted date range, and an AI-generated
 * summary via Gemini Nano. Handles [TripDetailUiState.Loading] and [TripDetailUiState.Error] states.
 */
@Composable
private fun TripDetailsTabContent(
    uiState: TripDetailUiState,
    innerPadding: PaddingValues,
) {
    when (uiState) {
        is TripDetailUiState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        is TripDetailUiState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.trip_detail_error),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        is TripDetailUiState.Success -> {
            val trip = uiState.trip
            val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                if (trip.imageUri != null) {
                    item {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(Uri.parse(trip.imageUri))
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(
                                R.string.trip_detail_cover_image_desc,
                                trip.title,
                            ),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                        )
                    }
                }
                item {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = trip.title,
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        if (trip.startDate != null && trip.endDate != null) {
                            Text(
                                text = "${trip.startDate.format(formatter)} – ${trip.endDate.format(formatter)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        AiDescriptionSection(descriptionState = uiState.descriptionState)
                    }
                }
            }
        }
    }
}

/**
 * Renders the AI-generated trip description section.
 *
 * Shows a section title ("AI Summary") followed by the current [DescriptionState]:
 * a spinner while loading, the generated text when available, or a graceful fallback message.
 */
@Composable
private fun AiDescriptionSection(descriptionState: DescriptionState) {
    Text(
        text = stringResource(R.string.trip_detail_ai_summary_title),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(modifier = Modifier.height(8.dp))
    when (descriptionState) {
        is DescriptionState.Loading -> {
            CircularProgressIndicator(modifier = Modifier.height(24.dp))
        }
        is DescriptionState.Available -> {
            Text(
                text = descriptionState.text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        is DescriptionState.Unavailable -> {
            Text(
                text = stringResource(R.string.trip_detail_ai_summary_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        is DescriptionState.Error -> {
            Text(
                text = stringResource(R.string.trip_detail_ai_summary_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailLoadingPreview() {
    WanderVaultTheme {
        TripDetailContent(uiState = TripDetailUiState.Loading, tripId = 0, onNavigateUp = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailErrorPreview() {
    WanderVaultTheme {
        TripDetailContent(uiState = TripDetailUiState.Error, tripId = 0, onNavigateUp = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailSuccessPreview() {
    val trip = Trip(
        id = 1,
        title = "Paris Getaway",
        startDate = LocalDate.of(2024, 6, 1),
        endDate = LocalDate.of(2024, 6, 10),
    )
    WanderVaultTheme {
        TripDetailContent(uiState = TripDetailUiState.Success(trip), tripId = 1, onNavigateUp = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailAiAvailablePreview() {
    val trip = Trip(
        id = 1,
        title = "Tokyo Adventure",
        startDate = LocalDate.of(2024, 9, 1),
        endDate = LocalDate.of(2024, 9, 15),
    )
    WanderVaultTheme {
        TripDetailContent(
            uiState = TripDetailUiState.Success(
                trip = trip,
                descriptionState = DescriptionState.Available(
                    "Tokyo Adventure is an exciting 15-day journey through Japan's vibrant capital. " +
                        "Explore ancient temples, futuristic technology, and world-class cuisine " +
                        "across one of the world's most dynamic cities.",
                ),
            ),
            tripId = 1,
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailAiUnavailablePreview() {
    val trip = Trip(
        id = 1,
        title = "Paris Getaway",
        startDate = LocalDate.of(2024, 6, 1),
        endDate = LocalDate.of(2024, 6, 10),
    )
    WanderVaultTheme {
        TripDetailContent(
            uiState = TripDetailUiState.Success(
                trip = trip,
                descriptionState = DescriptionState.Unavailable,
            ),
            tripId = 1,
            onNavigateUp = {},
        )
    }
}
