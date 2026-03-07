package cat.company.wandervault.ui.screens

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.ui.sharedTripCoverBounds
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

/** Height of the trip cover image when rendered below the top app bar. */
private val BASE_IMAGE_HEIGHT = 200.dp

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
        onRegenerateDescription = viewModel::regenerateDescription,
        onDeleteDescription = viewModel::deleteDescription,
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
    onRegenerateDescription: () -> Unit = {},
    onDeleteDescription: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(TripDetailTab.DETAILS) }

    val hasImage = uiState is TripDetailUiState.Success && uiState.trip.imageUri != null
    val isImageCoveringTopBar = hasImage && selectedTab == TripDetailTab.DETAILS

    // Always created so it can be unconditionally called (it's @Composable and manages its own
    // state); the nestedScroll connection and scrollBehavior are only wired into the Scaffold and
    // TopAppBar when the cover image extends behind the top bar.
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val scrolledFraction = if (isImageCoveringTopBar) scrollBehavior.state.overlappedFraction else 0f
    val navIconColor = if (isImageCoveringTopBar) {
        lerp(Color.White, MaterialTheme.colorScheme.onSurface, scrolledFraction)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Scaffold(
        modifier = if (isImageCoveringTopBar) {
            modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            modifier
        },
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    // Hide the title while the image is visible; show it once content scrolls
                    // behind the bar (scrolledFraction > 0) so the bar is opaque.
                    if (!isImageCoveringTopBar || scrolledFraction > 0f) {
                        Text(
                            if (uiState is TripDetailUiState.Success) {
                                uiState.trip.title
                            } else {
                                stringResource(R.string.trip_detail_title)
                            },
                        )
                    }
                },
                navigationIcon = {
                    if (!isImageCoveringTopBar || scrolledFraction > 0f) {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.trip_detail_navigate_up),
                                tint = navIconColor,
                            )
                        }
                    }
                },
                scrollBehavior = if (isImageCoveringTopBar) scrollBehavior else null,
                colors = if (isImageCoveringTopBar) {
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        navigationIconContentColor = navIconColor,
                    )
                } else {
                    TopAppBarDefaults.topAppBarColors()
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
            TripDetailTab.DETAILS -> TripDetailsTabContent(
                uiState = uiState,
                innerPadding = innerPadding,
                onNavigateUp = onNavigateUp,
                isImageCoveringTopBar = isImageCoveringTopBar,
                scrolledFraction = scrolledFraction,
                onRegenerateDescription = onRegenerateDescription,
                onDeleteDescription = onDeleteDescription,
            )
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
 *
 * When [isImageCoveringTopBar] is true the cover image extends behind the top app bar, so the
 * [LazyColumn] omits top padding and the image is sized to fill the full top-bar area.
 */
@Composable
private fun TripDetailsTabContent(
    uiState: TripDetailUiState,
    innerPadding: PaddingValues,
    onNavigateUp: () -> Unit,
    isImageCoveringTopBar: Boolean = false,
    scrolledFraction: Float = 0f,
    onRegenerateDescription: () -> Unit = {},
    onDeleteDescription: () -> Unit = {},
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
                contentPadding = if (isImageCoveringTopBar) {
                    PaddingValues(bottom = innerPadding.calculateBottomPadding())
                } else {
                    innerPadding
                },
            ) {
                if (trip.imageUri != null) {
                    item {
                        val imageHeight = if (isImageCoveringTopBar) {
                            BASE_IMAGE_HEIGHT + innerPadding.calculateTopPadding()
                        } else {
                            BASE_IMAGE_HEIGHT
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(imageHeight)
                                .sharedTripCoverBounds(trip.id),
                        ) {
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
                                modifier = Modifier.fillMaxSize(),
                            )
                            if (isImageCoveringTopBar && scrolledFraction <= 0f) {
                                IconButton(
                                    onClick = onNavigateUp,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .windowInsetsPadding(WindowInsets.statusBars),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.trip_detail_navigate_up),
                                        tint = Color.White,
                                    )
                                }
                            }
                        }
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
                        if (uiState.descriptionState !is DescriptionState.Unavailable) {
                            Spacer(modifier = Modifier.height(16.dp))
                            AiDescriptionSection(
                                descriptionState = uiState.descriptionState,
                                onRegenerate = onRegenerateDescription,
                                onDelete = onDeleteDescription,
                            )
                        }
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
 * a spinner while loading, or the generated text when available.
 * When the description is [DescriptionState.Available], Regenerate and Delete icon buttons are shown.
 * When the description is [DescriptionState.Error], a Regenerate icon button is shown.
 * When the description is [DescriptionState.None], a "Generate" button is shown.
 *
 * This composable must not be called when [descriptionState] is [DescriptionState.Unavailable];
 * callers should hide the section entirely in that case.
 */
@Composable
private fun AiDescriptionSection(
    descriptionState: DescriptionState,
    onRegenerate: () -> Unit = {},
    onDelete: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.trip_detail_ai_summary_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (descriptionState is DescriptionState.Available || descriptionState is DescriptionState.Error) {
            IconButton(onClick = onRegenerate) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.trip_detail_ai_summary_regenerate),
                )
            }
        }
        if (descriptionState is DescriptionState.Available) {
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.trip_detail_ai_summary_delete),
                )
            }
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    when (descriptionState) {
        is DescriptionState.Loading -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.trip_detail_ai_summary_generating),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        is DescriptionState.Available -> {
            Text(
                text = descriptionState.text,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        is DescriptionState.Error -> {
            Text(
                text = stringResource(R.string.trip_detail_ai_summary_error),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        is DescriptionState.None -> {
            TextButton(onClick = onRegenerate) {
                Text(text = stringResource(R.string.trip_detail_ai_summary_generate))
            }
        }
        is DescriptionState.Unavailable -> {
            error("AiDescriptionSection should not be called with DescriptionState.Unavailable. Hide the section at the call site instead.")
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
