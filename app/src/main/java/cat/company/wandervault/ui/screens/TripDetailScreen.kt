package cat.company.wandervault.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.Role
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Tabs shown in the Trip Detail bottom navigation bar. */
private enum class TripDetailTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    DETAILS(R.string.trip_detail_tab_details, Icons.Default.Info),
    ITINERARY(R.string.trip_detail_tab_itinerary, Icons.Default.DateRange),
    CALENDAR(R.string.trip_detail_tab_calendar, Icons.Default.CalendarMonth),
    DOCUMENTS(R.string.trip_detail_tab_documents, Icons.Default.Folder),
}

/** Height of the trip cover image when rendered below the top app bar. */
private val BASE_IMAGE_HEIGHT = 200.dp

/** Shared date formatter for the Trip Detail screen (medium style, e.g. "Sep 2, 2024"). */
private val DETAIL_DATE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

/** Shared time formatter for the Trip Detail screen (short style, e.g. "10:30 AM"). */
private val DETAIL_TIME_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)

/**
 * Trip Detail screen entry point.
 *
 * Fetches trip state via [TripDetailViewModel] and renders the detail content.
 *
 * @param tripId The ID of the trip to display.
 * @param onNavigateUp Called when the user taps the back/up button.
 * @param onNavigateToDestination Called when the user taps a destination in the itinerary or a stay in the calendar legend.
 * @param onNavigateToTransport Called when the user taps the transport icon in the itinerary.
 * @param onNavigateToDocument Called with the document ID when the user requests document info.
 * @param modifier Optional [Modifier].
 */
@Composable
fun TripDetailScreen(
    tripId: Int,
    onNavigateUp: () -> Unit,
    onNavigateToDestination: (Int) -> Unit = {},
    onNavigateToTransport: (Int) -> Unit = {},
    onNavigateToDocument: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TripDetailViewModel = koinViewModel(key = "TripDetailViewModel:$tripId", parameters = { parametersOf(tripId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onNavigateUp)
    TripDetailContent(
        uiState = uiState,
        tripId = tripId,
        onNavigateUp = onNavigateUp,
        onNavigateToDestination = onNavigateToDestination,
        onNavigateToTransport = onNavigateToTransport,
        onNavigateToDocument = onNavigateToDocument,
        onRegenerateDescription = viewModel::regenerateDescription,
        onDeleteDescription = viewModel::deleteDescription,
        onClearDescriptionSource = viewModel::onClearDescriptionSource,
        onRefreshWhatsNext = viewModel::refreshWhatsNext,
        modifier = modifier,
    )
}

/**
 * Stateless presentation of the Trip Detail screen.
 *
 * Accepts a [TripDetailUiState] snapshot so it can be reused in `@Preview` without a real ViewModel.
 *
 * @param tripId The ID of the trip – forwarded to [ItineraryTabContent] when the Itinerary tab is selected.
 * @param onNavigateToDestination Called when the user taps a destination in the itinerary tab or a stay in the calendar legend.
 * @param onNavigateToTransport Called when the user taps the transport icon in the itinerary tab.
 * @param onNavigateToDocument Called with the document ID when the user requests document info.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripDetailContent(
    uiState: TripDetailUiState,
    tripId: Int,
    onNavigateUp: () -> Unit,
    onNavigateToDestination: (Int) -> Unit = {},
    onNavigateToTransport: (Int) -> Unit = {},
    onNavigateToDocument: (Int) -> Unit = {},
    onRegenerateDescription: () -> Unit = {},
    onDeleteDescription: () -> Unit = {},
    onClearDescriptionSource: () -> Unit = {},
    onRefreshWhatsNext: () -> Unit = {},
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
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.trip_detail_navigate_up),
                            tint = navIconColor,
                        )
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
                isImageCoveringTopBar = isImageCoveringTopBar,
                onNavigateToDestination = onNavigateToDestination,
                onNavigateToTransport = onNavigateToTransport,
                onRegenerateDescription = onRegenerateDescription,
                onDeleteDescription = onDeleteDescription,
                onClearDescriptionSource = onClearDescriptionSource,
                onNavigateToDocument = onNavigateToDocument,
                onRefreshWhatsNext = onRefreshWhatsNext,
            )
            TripDetailTab.ITINERARY -> ItineraryTabContent(
                tripId = tripId,
                innerPadding = innerPadding,
                onDestinationClick = onNavigateToDestination,
                onTransportClick = onNavigateToTransport,
            )
            TripDetailTab.CALENDAR -> CalendarTabContent(
                tripId = tripId,
                innerPadding = innerPadding,
                onDestinationClick = onNavigateToDestination,
            )
            TripDetailTab.DOCUMENTS -> TripDocumentsTabContent(
                tripId = tripId,
                innerPadding = innerPadding,
                onNavigateToDocument = onNavigateToDocument,
            )
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
    isImageCoveringTopBar: Boolean = false,
    onNavigateToDestination: (Int) -> Unit = {},
    onNavigateToTransport: (Int) -> Unit = {},
    onRegenerateDescription: () -> Unit = {},
    onDeleteDescription: () -> Unit = {},
    onClearDescriptionSource: () -> Unit = {},
    onNavigateToDocument: (Int) -> Unit = {},
    onRefreshWhatsNext: () -> Unit = {},
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
            val formatter = DETAIL_DATE_FORMATTER
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
                            Spacer(modifier = Modifier.height(16.dp))
                            UpcomingEventsSection(
                                events = uiState.upcomingEvents,
                                onNavigateToDestination = onNavigateToDestination,
                                onNavigateToTransport = onNavigateToTransport,
                            )
                            WhatsNextSection(
                                whatsNextState = uiState.whatsNextState,
                                isAiAvailable = uiState.isAiAvailable,
                                onRefresh = onRefreshWhatsNext,
                            )
                            if (uiState.descriptionState !is DescriptionState.Unavailable) {
                                Spacer(modifier = Modifier.height(16.dp))
                                AiDescriptionSection(
                                    descriptionState = uiState.descriptionState,
                                    isAiAvailable = uiState.isAiAvailable,
                                    onRegenerate = onRegenerateDescription,
                                    onDelete = onDeleteDescription,
                                    onClearSource = onClearDescriptionSource,
                                    onNavigateToDocument = onNavigateToDocument,
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
 * Shows a card with a title ("AI Summary") followed by the current [DescriptionState]:
 * a spinner while loading, or the generated text when available.
 * When [isAiAvailable] is true and the description is [DescriptionState.Available] or
 * [DescriptionState.Error], a Regenerate icon button is shown.
 * When the description is [DescriptionState.Available], a Delete icon button is always shown.
 * When the description is [DescriptionState.None], a "Generate" button is shown.
 *
 * This composable must not be called when [descriptionState] is [DescriptionState.Unavailable];
 * callers should hide the section entirely in that case.
 */
@Composable
private fun AiDescriptionSection(
    descriptionState: DescriptionState,
    isAiAvailable: Boolean,
    onRegenerate: () -> Unit = {},
    onDelete: () -> Unit = {},
    onClearSource: () -> Unit = {},
    onNavigateToDocument: (Int) -> Unit = {},
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.trip_detail_ai_summary_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (isAiAvailable && (descriptionState is DescriptionState.Available || descriptionState is DescriptionState.Error)) {
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
                    val docId = descriptionState.sourceDocumentId
                    if (docId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SourceDocumentChip(
                            documentName = descriptionState.sourceDocumentName,
                            onDocumentClick = { onNavigateToDocument(docId) },
                            onRemove = onClearSource,
                        )
                    }
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
    }
}

/**
 * Renders the "Next Up" section, listing the upcoming itinerary events sorted by time.
 *
 * The section is hidden when [events] is empty. Each event shows its date/time and a label
 * indicating whether it is an arrival or departure and the destination name. Tapping an arrival
 * event navigates to the destination detail; tapping a departure event navigates to the transport
 * detail.
 */
@Composable
private fun UpcomingEventsSection(
    events: List<UpcomingEvent>,
    onNavigateToDestination: (Int) -> Unit = {},
    onNavigateToTransport: (Int) -> Unit = {},
) {
    if (events.isEmpty()) return

    val dateFormatter = DETAIL_DATE_FORMATTER
    val timeFormatter = DETAIL_TIME_FORMATTER

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.trip_detail_next_up_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.height(8.dp))
            events.forEachIndexed { index, event ->
                if (index > 0) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable(
                            role = Role.Button,
                            onClickLabel = when (event.eventType) {
                                UpcomingEvent.EventType.ARRIVAL ->
                                    stringResource(R.string.trip_detail_next_up_open_destination)
                                UpcomingEvent.EventType.DEPARTURE ->
                                    stringResource(R.string.trip_detail_next_up_open_transport)
                            },
                        ) {
                            when (event.eventType) {
                                UpcomingEvent.EventType.ARRIVAL ->
                                    onNavigateToDestination(event.destinationId)
                                UpcomingEvent.EventType.DEPARTURE ->
                                    onNavigateToTransport(event.destinationId)
                            }
                        }
                        .padding(vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = when (event.eventType) {
                            UpcomingEvent.EventType.ARRIVAL -> Icons.AutoMirrored.Filled.ArrowBack
                            UpcomingEvent.EventType.DEPARTURE -> Icons.AutoMirrored.Filled.ArrowForward
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Column {
                        Text(
                            text = when (event.eventType) {
                                UpcomingEvent.EventType.ARRIVAL ->
                                    stringResource(R.string.trip_detail_next_up_arrive, event.destinationName)
                                UpcomingEvent.EventType.DEPARTURE ->
                                    stringResource(R.string.trip_detail_next_up_depart, event.destinationName)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "${event.dateTime.format(dateFormatter)}, ${event.dateTime.format(timeFormatter)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

/**
 * Renders the AI-generated "What's Next" notice section.
 *
 * Shows a card with a title ("What's Next") and the current [WhatsNextState]:
 * a spinner while loading, or the generated notice when available.
 * A Refresh icon button is shown when [isAiAvailable] is true and the state is
 * [WhatsNextState.Available] or [WhatsNextState.Error].
 * The section is hidden entirely (returns immediately) when [WhatsNextState] is
 * [WhatsNextState.Unavailable] or [WhatsNextState.None].
 */
@Composable
private fun WhatsNextSection(
    whatsNextState: WhatsNextState,
    isAiAvailable: Boolean,
    onRefresh: () -> Unit = {},
) {
    // Render nothing for terminal non-display states.
    if (whatsNextState is WhatsNextState.Unavailable || whatsNextState is WhatsNextState.None) return

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.trip_detail_whats_next_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (isAiAvailable &&
                    (whatsNextState is WhatsNextState.Available || whatsNextState is WhatsNextState.Error)
                ) {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = stringResource(
                                R.string.trip_detail_whats_next_refresh,
                            ),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            when (whatsNextState) {
                is WhatsNextState.Loading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.trip_detail_whats_next_generating),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                is WhatsNextState.Available -> {
                    Text(
                        text = whatsNextState.text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                is WhatsNextState.Error -> {
                    Text(
                        text = stringResource(R.string.trip_detail_whats_next_error),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                is WhatsNextState.Unavailable,
                is WhatsNextState.None,
                -> Unit // Already handled by the early return above.
            }
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
                isAiAvailable = true,
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

@Preview(showBackground = true)
@Composable
private fun TripDetailWhatsNextAvailablePreview() {
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
                descriptionState = DescriptionState.Unavailable,
                whatsNextState = WhatsNextState.Available(
                    "Your flight to Tokyo departs tomorrow at 10:30 AM JST. Get ready for an " +
                        "amazing adventure in Japan's vibrant capital!",
                ),
                isAiAvailable = true,
            ),
            tripId = 1,
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailWhatsNextLoadingPreview() {
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
                descriptionState = DescriptionState.Unavailable,
                whatsNextState = WhatsNextState.Loading,
            ),
            tripId = 1,
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TripDetailNextUpPreview() {
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
                descriptionState = DescriptionState.Unavailable,
                upcomingEvents = listOf(
                    UpcomingEvent(
                        dateTime = ZonedDateTime.parse("2024-09-02T10:30:00+09:00[Asia/Tokyo]"),
                        destinationName = "Tokyo",
                        destinationId = 1,
                        eventType = UpcomingEvent.EventType.ARRIVAL,
                    ),
                    UpcomingEvent(
                        dateTime = ZonedDateTime.parse("2024-09-10T14:00:00+09:00[Asia/Tokyo]"),
                        destinationName = "Tokyo",
                        destinationId = 1,
                        eventType = UpcomingEvent.EventType.DEPARTURE,
                    ),
                    UpcomingEvent(
                        dateTime = ZonedDateTime.parse("2024-09-10T18:30:00+09:00[Asia/Tokyo]"),
                        destinationName = "Osaka",
                        destinationId = 2,
                        eventType = UpcomingEvent.EventType.ARRIVAL,
                    ),
                ),
            ),
            tripId = 1,
            onNavigateUp = {},
        )
    }
}
