package cat.company.wandervault.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WrongLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.ui.theme.WanderVaultTheme
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Width of the timeline rail column; transport nodes use it as their diameter. */
private const val TIMELINE_COLUMN_WIDTH_DP = 32

/** Vertical position of a destination node's centre, aligned with the card's title row. */
private const val TIMELINE_NODE_CENTER_DP = 36

/** Size of the box that holds a destination node (including the "current" halo). */
private const val TIMELINE_NODE_BOX_DP = 28

/** Stroke width of the timeline rail. */
private const val TIMELINE_STROKE_DP = 3

/**
 * Stateful entry point for the Itinerary tab.
 *
 * Retrieves [ItineraryViewModel] via Koin and delegates rendering to [ItineraryContent].
 *
 * @param tripId The ID of the trip whose itinerary is shown.
 */
@Composable
internal fun ItineraryTabContent(
    tripId: Int,
    innerPadding: PaddingValues,
    onDestinationClick: (Int) -> Unit = {},
    onTransportClick: (Int) -> Unit = {},
    viewModel: ItineraryViewModel = koinViewModel(key = "ItineraryViewModel:$tripId", parameters = { parametersOf(tripId) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ItineraryContent(
        uiState = uiState,
        innerPadding = innerPadding,
        onAddDestinationClick = { viewModel.onAddDestinationClick() },
        onDismissAddDestinationDialog = viewModel::onDismissAddDestinationDialog,
        onNewDestinationNameChange = viewModel::onNewDestinationNameChange,
        onSaveDestination = viewModel::onSaveDestination,
        onUpdateArrivalDateTime = viewModel::onUpdateArrivalDateTime,
        onUpdateDepartureDateTime = viewModel::onUpdateDepartureDateTime,
        onDeleteDestination = viewModel::onDeleteDestination,
        onDismissDeleteDestinationDialog = viewModel::onDismissDeleteDestinationDialog,
        onConfirmDeleteDestination = viewModel::onConfirmDeleteDestination,
        onMoveDestinationUp = viewModel::onMoveDestinationUp,
        onMoveDestinationDown = viewModel::onMoveDestinationDown,
        onAddDestinationAfter = { pos -> viewModel.onAddDestinationClick(pos) },
        onShowTimezoneRangeDialog = viewModel::onShowTimezoneRangeDialog,
        onDismissTimezoneRangeDialog = viewModel::onDismissTimezoneRangeDialog,
        onApplyTimezoneToRange = viewModel::onApplyTimezoneToRange,
        onTransportClick = onTransportClick,
        onDestinationClick = onDestinationClick,
    )
}

/**
 * Stateless itinerary content composable.
 *
 * Renders a vertical timeline of destinations with date/time pickers for
 * arrival and departure, filtered by each destination's position (first/last/intermediate).
 */
@Composable
internal fun ItineraryContent(
    uiState: ItineraryUiState,
    innerPadding: PaddingValues,
    onAddDestinationClick: () -> Unit,
    onDismissAddDestinationDialog: () -> Unit,
    onNewDestinationNameChange: (String) -> Unit,
    onSaveDestination: () -> Unit,
    onUpdateArrivalDateTime: (Destination, ZonedDateTime?) -> Unit,
    onUpdateDepartureDateTime: (Destination, ZonedDateTime?) -> Unit,
    onDeleteDestination: (Destination) -> Unit,
    onDismissDeleteDestinationDialog: () -> Unit,
    onConfirmDeleteDestination: () -> Unit,
    onMoveDestinationUp: (Destination) -> Unit,
    onMoveDestinationDown: (Destination) -> Unit,
    onAddDestinationAfter: (Int) -> Unit,
    onShowTimezoneRangeDialog: () -> Unit = {},
    onDismissTimezoneRangeDialog: () -> Unit = {},
    onApplyTimezoneToRange: (Int, Int, String?) -> Unit = { _, _, _ -> },
    onTransportClick: (Int) -> Unit = {},
    onDestinationClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // Drive 'now' from a state that updates every minute so departure/arrival transitions
    // are reflected promptly and the marker stays accurate after midnight or timezone changes.
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            delay(60_000L)
            value = Instant.now()
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            uiState.destinations.isEmpty() -> ItineraryEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                itemsIndexed(uiState.destinations, key = { _, destination -> destination.id }) { index, destination ->
                    DestinationTimelineItem(
                        destination = destination,
                        isFirst = index == 0,
                        isLast = index == uiState.destinations.lastIndex,
                        previousDestination = uiState.destinations.getOrNull(index - 1),
                        nextDestination = uiState.destinations.getOrNull(index + 1),
                        today = now,
                        onUpdateArrivalDateTime = { dt -> onUpdateArrivalDateTime(destination, dt) },
                        onUpdateDepartureDateTime = { dt -> onUpdateDepartureDateTime(destination, dt) },
                        onDeleteDestination = { onDeleteDestination(destination) },
                        onMoveUp = { onMoveDestinationUp(destination) },
                        onMoveDown = { onMoveDestinationDown(destination) },
                        onAddAfter = { onAddDestinationAfter(destination.position) },
                        onTransportClick = { onTransportClick(destination.id) },
                        onClick = { onDestinationClick(destination.id) },
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.destinations.size >= 2) {
                SmallFloatingActionButton(onClick = onShowTimezoneRangeDialog) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = stringResource(R.string.itinerary_change_timezone_range),
                    )
                }
            }
            FloatingActionButton(onClick = onAddDestinationClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.itinerary_add_destination))
            }
        }
    }

    if (uiState.showAddDestinationDialog) {
        AddDestinationDialog(
            name = uiState.newDestinationName,
            onNameChange = onNewDestinationNameChange,
            isFormValid = uiState.isAddDestinationFormValid,
            onSave = onSaveDestination,
            onDismiss = onDismissAddDestinationDialog,
        )
    }

    if (uiState.destinationPendingDelete != null) {
        DeleteDestinationConfirmationDialog(
            destinationName = uiState.destinationPendingDelete.name,
            onConfirm = onConfirmDeleteDestination,
            onDismiss = onDismissDeleteDestinationDialog,
        )
    }

    if (uiState.showTimezoneRangeDialog) {
        TimezoneRangeDialog(
            destinations = uiState.destinations,
            onApply = { fromIndex, toIndex, zoneId ->
                onApplyTimezoneToRange(fromIndex, toIndex, zoneId)
                onDismissTimezoneRangeDialog()
            },
            onDismiss = onDismissTimezoneRangeDialog,
        )
    }
}

@Composable
private fun DestinationTimelineItem(
    destination: Destination,
    isFirst: Boolean,
    isLast: Boolean,
    previousDestination: Destination?,
    nextDestination: Destination?,
    today: Instant,
    onUpdateArrivalDateTime: (ZonedDateTime?) -> Unit,
    onUpdateDepartureDateTime: (ZonedDateTime?) -> Unit,
    onDeleteDestination: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddAfter: () -> Unit,
    onTransportClick: () -> Unit = {},
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val prevDepartureMillis = previousDestination?.departureDateTime.toDateEpochMillis()
    val nextArrivalMillis = nextDestination?.arrivalDateTime.toDateEpochMillis()
    val ownArrivalMillis = destination.arrivalDateTime.toDateEpochMillis()
    val ownDepartureMillis = destination.departureDateTime.toDateEpochMillis()

    // Compare using Instant so that destination timezone is respected and same-day
    // departure/arrival is handled correctly (e.g. depart 08:00, arrive 22:00 same day).
    val arrivalInstant = destination.arrivalDateTime?.toInstant()
    val departureInstant = destination.departureDateTime?.toInstant()

    // True when the current instant falls within this destination's stay.
    // When both bounds are known, now must be in [arrival, departure].
    // For the first destination (no arrival expected), now must be at or before departure.
    // For the last destination (no departure expected), now must be at or after arrival.
    // In all other cases both instants must be present to avoid false-positive markers.
    val isTodayInStay = when {
        arrivalInstant != null && departureInstant != null ->
            !today.isBefore(arrivalInstant) && !today.isAfter(departureInstant)
        isFirst && departureInstant != null ->
            !today.isAfter(departureInstant)
        isLast && arrivalInstant != null ->
            !today.isBefore(arrivalInstant)
        else -> false
    }

    // True when the current instant is strictly between this destination's departure and
    // the next destination's arrival (i.e. we are currently in transit).
    val isTodayInTransport: Boolean = run {
        if (isLast || departureInstant == null) return@run false
        val nextArrivalInstant = nextDestination?.arrivalDateTime?.toInstant() ?: return@run false
        today.isAfter(departureInstant) && today.isBefore(nextArrivalInstant)
    }

    var showStayDurationInDays by rememberSaveable { mutableStateOf(false) }
    var showTransportDurationInDays by rememberSaveable { mutableStateOf(false) }
    var showActionsMenu by remember { mutableStateOf(false) }

    val railColor = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Section 1: destination node + destination card (name, dates, stay duration) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
        ) {
            // Timeline rail: line in from the previous transport → node → line out to transport.
            Box(
                modifier = Modifier
                    .width(TIMELINE_COLUMN_WIDTH_DP.dp)
                    .fillMaxHeight()
                    .drawBehind {
                        val x = size.width / 2
                        val nodeY = TIMELINE_NODE_CENTER_DP.dp.toPx()
                        val stroke = TIMELINE_STROKE_DP.dp.toPx()
                        if (!isFirst) {
                            drawLine(railColor, Offset(x, 0f), Offset(x, nodeY), stroke)
                        }
                        if (!isLast) {
                            drawLine(railColor, Offset(x, nodeY), Offset(x, size.height), stroke)
                        }
                    },
            ) {
                DestinationNode(
                    isCurrent = isTodayInStay,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = (TIMELINE_NODE_CENTER_DP - TIMELINE_NODE_BOX_DP / 2).dp),
                )
            }

            // Destination card
            val cardShape = CardDefaults.shape
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
                    .clip(cardShape)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = stringResource(R.string.itinerary_destination_open_details),
                        onClick = onClick,
                    ),
                shape = cardShape,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                border = if (isTodayInStay) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
                } else {
                    null
                },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp, end = 4.dp)
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = destination.name,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        // "Today" chip — shown when today falls within this destination's stay.
                        if (isTodayInStay) {
                            Spacer(modifier = Modifier.width(8.dp))
                            TodayChip()
                        }
                    }
                    Box {
                        IconButton(onClick = { showActionsMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.itinerary_destination_actions),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false },
                        ) {
                            if (!isFirst) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.itinerary_move_up)) },
                                    onClick = { onMoveUp(); showActionsMenu = false },
                                    leadingIcon = {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                                    },
                                )
                            }
                            if (!isLast) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.itinerary_move_down)) },
                                    onClick = { onMoveDown(); showActionsMenu = false },
                                    leadingIcon = {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(R.string.itinerary_delete_destination),
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                },
                                onClick = { onDeleteDestination(); showActionsMenu = false },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                },
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // Show arrival only for non-first destinations (hidden when there is only one place)
                    if (!isFirst) {
                        DateTimeField(
                            label = stringResource(R.string.itinerary_arrival_label),
                            labelIcon = Icons.AutoMirrored.Outlined.Login,
                            dateTime = destination.arrivalDateTime,
                            onDateTimeChange = onUpdateArrivalDateTime,
                            minDateMillis = prevDepartureMillis,
                            maxDateMillis = ownDepartureMillis,
                        )
                    }
                    // Show departure only for non-last destinations (hidden when there is only one place)
                    if (!isLast) {
                        DateTimeField(
                            label = stringResource(R.string.itinerary_departure_label),
                            labelIcon = Icons.AutoMirrored.Outlined.Logout,
                            dateTime = destination.departureDateTime,
                            onDateTimeChange = onUpdateDepartureDateTime,
                            minDateMillis = listOfNotNull(ownArrivalMillis, prevDepartureMillis).maxOrNull(),
                            maxDateMillis = nextArrivalMillis,
                        )
                    }

                    // Show stay duration when both arrival and departure are set.
                    val stayDuration = destination.arrivalDateTime.durationUntil(destination.departureDateTime)
                    if (stayDuration != null) {
                        DurationPill(
                            text = stringResource(
                                R.string.itinerary_stay_duration,
                                if (showStayDurationInDays) stayDuration.formattedWithDays() else stayDuration.formatted(),
                            ),
                            icon = Icons.Outlined.Hotel,
                            onClick = { showStayDurationInDays = !showStayDurationInDays },
                        )
                    }
                }
            }
        }

        // ── Section 2: transport node + transport summary ──────────────────────────────
        if (!isLast) {
            val hasTransport = destination.transport != null && destination.transport.legs.isNotEmpty()
            val pendingRailColor = MaterialTheme.colorScheme.outlineVariant
            val transportLabel = stringResource(
                if (hasTransport) R.string.itinerary_change_transport else R.string.itinerary_add_transport,
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        // Solid rail when transport is planned, dashed when it is still missing.
                        val x = (TIMELINE_COLUMN_WIDTH_DP / 2).dp.toPx()
                        val stroke = TIMELINE_STROKE_DP.dp.toPx()
                        drawLine(
                            color = if (hasTransport) railColor else pendingRailColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = stroke,
                            pathEffect = if (hasTransport) {
                                null
                            } else {
                                PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
                            },
                        )
                    },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Transport node: filled when transport legs are set, outlined otherwise
                    Box(
                        modifier = Modifier.width(TIMELINE_COLUMN_WIDTH_DP.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(TIMELINE_COLUMN_WIDTH_DP.dp)
                                .clip(CircleShape)
                                .background(
                                    color = if (hasTransport) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    },
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (hasTransport) Color.Transparent else pendingRailColor,
                                    shape = CircleShape,
                                )
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = transportLabel,
                                    onClick = onTransportClick,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = destination.transport?.legs?.timelineLeg()?.type?.icon ?: Icons.Default.Add,
                                contentDescription = transportLabel,
                                modifier = Modifier.size(18.dp),
                                tint = if (hasTransport) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }

                    // Transport summary
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                    ) {
                        val legs = destination.transport?.legs
                        if (!legs.isNullOrEmpty()) {
                            TransportSummary(
                                legs = legs,
                                durationText = destination.departureDateTime
                                    .durationUntil(nextDestination?.arrivalDateTime)
                                    ?.let { duration ->
                                        // Using the destination-level date-times (rather than the
                                        // individual leg fields) ensures the duration is always
                                        // visible for single-leg transports whose leg dates may not
                                        // have been explicitly persisted to the database.
                                        stringResource(
                                            R.string.itinerary_transport_duration,
                                            if (showTransportDurationInDays) duration.formattedWithDays() else duration.formatted(),
                                        )
                                    },
                                onDurationClick = { showTransportDurationInDays = !showTransportDurationInDays },
                                isCurrent = isTodayInTransport,
                                onClick = onTransportClick,
                            )
                        } else {
                            TextButton(onClick = onTransportClick) {
                                Text(
                                    text = transportLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
                // "Add destination here" button shown between this item and the next
                TextButton(
                    onClick = onAddAfter,
                    modifier = Modifier.padding(start = (TIMELINE_COLUMN_WIDTH_DP + 4).dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.itinerary_add_destination_here),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
    }
}

/**
 * The node drawn on the timeline rail for a destination: a ring in the primary colour, or a
 * filled secondary dot with a soft halo when the user is currently at this destination.
 */
@Composable
private fun DestinationNode(isCurrent: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.size(TIMELINE_NODE_BOX_DP.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f), CircleShape),
            )
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

/**
 * Tappable card summarising a destination's onward transport: one line per leg (type, booking
 * details and intermediate stop) plus the total travel time.
 */
@Composable
private fun TransportSummary(
    legs: List<TransportLeg>,
    durationText: String?,
    onDurationClick: () -> Unit,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (isCurrent) BorderStroke(1.dp, MaterialTheme.colorScheme.secondary) else null,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val legIconSize = 16.dp
            val legIconSpacing = 8.dp
            legs.forEach { leg ->
                val typeLabel = stringResource(leg.type.labelRes)
                val bookingDetails = listOfNotNull(
                    leg.company,
                    leg.flightNumber,
                    leg.reservationConfirmationNumber,
                ).filter { it.isNotBlank() }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(legIconSpacing),
                ) {
                    Icon(
                        imageVector = leg.type.icon,
                        contentDescription = null,
                        modifier = Modifier.size(legIconSize),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (bookingDetails.isNotEmpty()) {
                        Text(
                            text = bookingDetails.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                // Show the stop name (intermediate destination) after booking details
                leg.stopName?.takeIf { it.isNotBlank() }?.let { stopName ->
                    Text(
                        text = stopName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = legIconSize + legIconSpacing),
                    )
                }
            }
            if (durationText != null || isCurrent) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (durationText != null) {
                        DurationPill(
                            text = durationText,
                            icon = Icons.Outlined.Timer,
                            onClick = onDurationClick,
                        )
                    }
                    // "Today" chip — shown when today falls in the transit window after this
                    // destination's departure and before the next destination's arrival.
                    if (isCurrent) {
                        TodayChip()
                    }
                }
            }
        }
    }
}

/**
 * Small tonal pill showing a duration. Tapping it toggles the duration format (e.g. hours vs
 * days + hours).
 */
@Composable
private fun DurationPill(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.itinerary_duration_toggle),
                onClick = onClick,
            ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(14.dp))
            Text(text = text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** Maps a [TransportType] to its corresponding [ImageVector] icon. */
private val TransportType.icon: ImageVector
    get() = when (this) {
        TransportType.WALKING -> Icons.Default.DirectionsWalk
        TransportType.CYCLING -> Icons.Default.DirectionsBike
        TransportType.DRIVING -> Icons.Default.DirectionsCar
        TransportType.BUS -> Icons.Default.DirectionsBus
        TransportType.TRAIN -> Icons.Default.Train
        TransportType.FERRY -> Icons.Default.DirectionsBoat
        TransportType.FLIGHT -> Icons.Default.Flight
        TransportType.OTHER -> Icons.Default.MoreHoriz
    }

/**
 * Returns the leg that should be represented in the itinerary timeline: the leg explicitly
 * marked as default, or the first leg if none is marked.
 */
private fun List<TransportLeg>.timelineLeg(): TransportLeg? = firstOrNull { it.isDefault } ?: firstOrNull()

/** Returns the string resource ID for the human-readable label of a [TransportType]. */
private val TransportType.labelRes: Int
    get() = when (this) {
        TransportType.WALKING -> R.string.transport_walking
        TransportType.CYCLING -> R.string.transport_cycling
        TransportType.DRIVING -> R.string.transport_driving
        TransportType.BUS -> R.string.transport_bus
        TransportType.TRAIN -> R.string.transport_train
        TransportType.FERRY -> R.string.transport_ferry
        TransportType.FLIGHT -> R.string.transport_flight
        TransportType.OTHER -> R.string.transport_other
    }

/**
 * A small pill-shaped chip labelled "Today", filled with [MaterialTheme.colorScheme.secondary].
 * Used on the itinerary timeline to indicate the user's current position in the trip, whether
 * they are at a destination stay or in transit between two destinations.
 */
@Composable
private fun TodayChip(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.itinerary_today),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondary,
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.secondary,
                shape = CircleShape,
            )
            .padding(horizontal = 10.dp, vertical = 2.dp),
    )
}

/** Empty-state message shown when a trip has no destinations yet. */
@Composable
private fun ItineraryEmptyState(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Outlined.Map,
        title = stringResource(R.string.itinerary_empty_title),
        message = stringResource(R.string.itinerary_empty_subtitle),
        modifier = modifier,
    )
}

/** Dialog for entering the name of a new destination. */
@Composable
private fun AddDestinationDialog(
    name: String,
    onNameChange: (String) -> Unit,
    isFormValid: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { DialogIcon(Icons.Outlined.AddLocationAlt) },
        title = { Text(stringResource(R.string.itinerary_add_destination_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.itinerary_destination_name_label)) },
                leadingIcon = { Icon(Icons.Outlined.Place, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { if (isFormValid) onSave() }),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = isFormValid) {
                Text(stringResource(R.string.add_trip_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/** Confirmation dialog shown before permanently removing a destination from the itinerary. */
@Composable
private fun DeleteDestinationConfirmationDialog(
    destinationName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DestructiveConfirmationDialog(
        title = stringResource(R.string.itinerary_delete_destination_dialog_title),
        message = stringResource(R.string.itinerary_delete_destination_dialog_message, destinationName),
        confirmLabel = stringResource(R.string.itinerary_delete_destination_dialog_confirm),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        icon = Icons.Outlined.WrongLocation,
    )
}

/**
 * Dialog that lets the user pick a consecutive range of destinations and a timezone, then
 * applies [ZonedDateTime.withZoneSameLocal] to all arrival/departure date-times in that range.
 *
 * @param destinations Ordered list of destinations shown in the from/to dropdowns.
 * @param onApply Called with (fromIndex, toIndex, zoneId) when the user confirms.
 *                [zoneId] is `null` for "device default".
 * @param onDismiss Called when the dialog is dismissed without applying.
 */
@Composable
private fun TimezoneRangeDialog(
    destinations: List<Destination>,
    onApply: (fromIndex: Int, toIndex: Int, zoneId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var fromIndex by rememberSaveable { mutableIntStateOf(0) }
    var toIndex by rememberSaveable { mutableIntStateOf(destinations.lastIndex.coerceAtLeast(0)) }
    var selectedZoneId by rememberSaveable { mutableStateOf<String?>(null) }
    var hasSelectedTimezone by rememberSaveable { mutableStateOf(false) }
    var showTimezonePicker by rememberSaveable { mutableStateOf(false) }

    // Clamp indices whenever the destination count changes (e.g. a destination is added or
    // removed while this dialog is open, or after process recreation).
    LaunchedEffect(destinations.size) {
        val lastIdx = destinations.lastIndex.coerceAtLeast(0)
        fromIndex = fromIndex.coerceIn(0, lastIdx)
        toIndex = toIndex.coerceIn(fromIndex, lastIdx)
    }

    if (showTimezonePicker) {
        TimezonePickerDialog(
            selectedZoneId = selectedZoneId,
            isDeviceDefaultSelected = hasSelectedTimezone && selectedZoneId == null,
            onTimezoneSelected = { zoneId ->
                selectedZoneId = zoneId
                hasSelectedTimezone = true
                showTimezonePicker = false
            },
            onDismiss = { showTimezonePicker = false },
        )
    }

    val deviceDefault = remember { ZoneId.systemDefault().id }
    val zoneIdSnapshot = selectedZoneId
    val timezoneButtonLabel = when {
        !hasSelectedTimezone -> stringResource(R.string.itinerary_timezone_range_select_timezone)
        zoneIdSnapshot == null -> stringResource(R.string.trip_timezone_device_default, deviceDefault)
        else -> zoneIdSnapshot
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { DialogIcon(Icons.Outlined.Public) },
        title = { Text(stringResource(R.string.itinerary_change_timezone_range_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DestinationRangeDropdown(
                    label = stringResource(R.string.itinerary_timezone_range_from),
                    destinations = destinations,
                    selectedIndex = fromIndex,
                    onIndexSelected = { fromIndex = it },
                )
                DestinationRangeDropdown(
                    label = stringResource(R.string.itinerary_timezone_range_to),
                    destinations = destinations,
                    selectedIndex = toIndex,
                    onIndexSelected = { toIndex = it },
                )
                SelectorField(
                    label = stringResource(R.string.itinerary_timezone_range_timezone),
                    value = timezoneButtonLabel,
                    leadingIcon = Icons.Outlined.Schedule,
                    isPlaceholder = !hasSelectedTimezone,
                    onClick = { showTimezonePicker = true },
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(fromIndex, toIndex, selectedZoneId) },
                enabled = hasSelectedTimezone && fromIndex <= toIndex,
            ) {
                Text(stringResource(R.string.itinerary_timezone_range_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/**
 * A labelled [SelectorField] that opens a [DropdownMenu] listing all destinations.
 * Used inside [TimezoneRangeDialog] to pick the "from" and "to" endpoints.
 */
@Composable
private fun DestinationRangeDropdown(
    label: String,
    destinations: List<Destination>,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxWidth()) {
        SelectorField(
            label = label,
            value = destinations.getOrNull(selectedIndex)?.name.orEmpty(),
            leadingIcon = Icons.Outlined.Place,
            onClick = { expanded = true },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            destinations.forEachIndexed { index, destination ->
                DropdownMenuItem(
                    text = { Text(destination.name) },
                    onClick = {
                        onIndexSelected(index)
                        expanded = false
                    },
                    leadingIcon = if (index == selectedIndex) {
                        { Icon(Icons.Default.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItineraryEmptyPreview() {
    WanderVaultTheme {
        ItineraryContent(
            uiState = ItineraryUiState(isLoading = false),
            innerPadding = PaddingValues(),
            onAddDestinationClick = {},
            onDismissAddDestinationDialog = {},
            onNewDestinationNameChange = {},
            onSaveDestination = {},
            onUpdateArrivalDateTime = { _, _ -> },
            onUpdateDepartureDateTime = { _, _ -> },
            onDeleteDestination = {},
            onDismissDeleteDestinationDialog = {},
            onConfirmDeleteDestination = {},
            onMoveDestinationUp = {},
            onMoveDestinationDown = {},
            onAddDestinationAfter = {},
            onTransportClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItineraryWithDestinationsPreview() {
    val destinations = listOf(
        Destination(1, 1, "London", 0, departureDateTime = ZonedDateTime.of(2024, 6, 1, 9, 0, 0, 0, ZoneId.of("Europe/London")), transport = Transport(id = 1, destinationId = 1, legs = listOf(TransportLeg(transportId = 1, type = TransportType.FLIGHT)))),
        Destination(
            2,
            1,
            "Paris",
            1,
            arrivalDateTime = ZonedDateTime.of(2024, 6, 1, 12, 30, 0, 0, ZoneId.of("Europe/Paris")),
            departureDateTime = ZonedDateTime.of(2024, 6, 3, 10, 0, 0, 0, ZoneId.of("Europe/Paris")),
            transport = Transport(id = 2, destinationId = 2, legs = listOf(TransportLeg(transportId = 2, type = TransportType.TRAIN))),
        ),
        Destination(3, 1, "Rome", 2, arrivalDateTime = ZonedDateTime.of(2024, 6, 3, 14, 0, 0, 0, ZoneId.of("Europe/Rome"))),
    )
    WanderVaultTheme {
        ItineraryContent(
            uiState = ItineraryUiState(destinations = destinations, isLoading = false),
            innerPadding = PaddingValues(),
            onAddDestinationClick = {},
            onDismissAddDestinationDialog = {},
            onNewDestinationNameChange = {},
            onSaveDestination = {},
            onUpdateArrivalDateTime = { _, _ -> },
            onUpdateDepartureDateTime = { _, _ -> },
            onDeleteDestination = {},
            onDismissDeleteDestinationDialog = {},
            onConfirmDeleteDestination = {},
            onMoveDestinationUp = {},
            onMoveDestinationDown = {},
            onAddDestinationAfter = {},
            onTransportClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItinerarySingleDestinationPreview() {
    val destinations = listOf(
        Destination(1, 1, "Tokyo", 0),
    )
    WanderVaultTheme {
        ItineraryContent(
            uiState = ItineraryUiState(destinations = destinations, isLoading = false),
            innerPadding = PaddingValues(),
            onAddDestinationClick = {},
            onDismissAddDestinationDialog = {},
            onNewDestinationNameChange = {},
            onSaveDestination = {},
            onUpdateArrivalDateTime = { _, _ -> },
            onUpdateDepartureDateTime = { _, _ -> },
            onDeleteDestination = {},
            onDismissDeleteDestinationDialog = {},
            onConfirmDeleteDestination = {},
            onMoveDestinationUp = {},
            onMoveDestinationDown = {},
            onAddDestinationAfter = {},
            onTransportClick = {},
        )
    }
}
