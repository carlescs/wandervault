package cat.company.wandervault.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
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
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private const val MILLIS_PER_DAY = 86_400_000L

/** Converts a [ZonedDateTime] to epoch-day milliseconds used by the [DatePicker] API. */
private fun ZonedDateTime?.toDateEpochMillis(): Long? =
    this?.toLocalDate()?.toEpochDay()?.times(MILLIS_PER_DAY)

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

    var showStayDurationInDays by rememberSaveable { mutableStateOf(false) }
    var showTransportDurationInDays by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // ── Section 1: destination dot + destination info (name, dates, stay duration) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
        ) {
            // Timeline: top line → destination dot → connector line to transport section
            Column(
                modifier = Modifier
                    .width(32.dp)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(if (isFirst) 8.dp else 16.dp)
                        .background(if (isFirst) Color.Transparent else MaterialTheme.colorScheme.primary),
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                )
                if (!isLast) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .weight(1f)
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }

            // Destination info content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, bottom = if (isLast) 8.dp else 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = destination.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.itinerary_destination_open_details),
                                onClick = onClick,
                            ),
                    )
                    IconButton(onClick = onMoveUp, enabled = !isFirst) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = stringResource(R.string.itinerary_move_up),
                            tint = if (isFirst) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = stringResource(R.string.itinerary_move_down),
                            tint = if (isLast) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDeleteDestination) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.itinerary_delete_destination),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Show arrival only for non-first destinations (hidden when there is only one place)
                if (!isFirst) {
                    DateTimeRow(
                        label = stringResource(R.string.itinerary_arrival_label),
                        dateTime = destination.arrivalDateTime,
                        onDateTimeChange = onUpdateArrivalDateTime,
                        minDateMillis = prevDepartureMillis,
                        maxDateMillis = ownDepartureMillis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                // Show departure only for non-last destinations (hidden when there is only one place)
                if (!isLast) {
                    DateTimeRow(
                        label = stringResource(R.string.itinerary_departure_label),
                        dateTime = destination.departureDateTime,
                        onDateTimeChange = onUpdateDepartureDateTime,
                        minDateMillis = listOfNotNull(ownArrivalMillis, prevDepartureMillis).maxOrNull(),
                        maxDateMillis = nextArrivalMillis,
                    )
                }

                // Show stay duration when both arrival and departure are set.
                val stayDuration = destination.arrivalDateTime.durationUntil(destination.departureDateTime)
                if (stayDuration != null) {
                    Text(
                        text = stringResource(
                            R.string.itinerary_stay_duration,
                            if (showStayDurationInDays) stayDuration.formattedWithDays() else stayDuration.formatted(),
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .heightIn(min = 48.dp)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.itinerary_duration_toggle),
                                onClick = { showStayDurationInDays = !showStayDurationInDays },
                            ),
                    )
                }
            }
        }

        // ── Section 2: transport circle + transport info ──────────────────────────────
        if (!isLast) {
            val hasTransport = destination.transport != null && destination.transport.legs.isNotEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
            ) {
                // Timeline: full-height line with transport circle overlaid at the centre
                Box(
                    modifier = Modifier
                        .width(32.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    // Transport circle: filled when transport legs are set, dimmed outline otherwise
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = if (hasTransport) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                shape = CircleShape,
                            )
                            .clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(
                                    if (hasTransport) R.string.itinerary_change_transport
                                    else R.string.itinerary_add_transport,
                                ),
                                onClick = onTransportClick,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = destination.transport?.legs?.timelineLeg()?.type?.icon ?: Icons.Default.Add,
                            contentDescription = stringResource(
                                if (hasTransport) R.string.itinerary_change_transport
                                else R.string.itinerary_add_transport,
                            ),
                            modifier = Modifier.size(18.dp),
                            tint = if (hasTransport) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            },
                        )
                    }
                }

                // Transport info content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    // Show transport details for all legs when set
                    val legIconSize = 12.dp
                    val legIconSpacing = 4.dp
                    destination.transport?.legs?.forEach { leg ->
                        // Show the type icon + type label + booking details first
                        val typeLabel = stringResource(leg.type.labelRes)
                        val bookingDetails = listOfNotNull(
                            leg.company,
                            leg.flightNumber,
                            leg.reservationConfirmationNumber,
                        )
                        val rowText = if (bookingDetails.isNotEmpty()) {
                            "$typeLabel · ${bookingDetails.joinToString(" · ")}"
                        } else {
                            typeLabel
                        }
                        Row(
                            modifier = Modifier.padding(top = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(legIconSpacing),
                        ) {
                            Icon(
                                imageVector = leg.type.icon,
                                contentDescription = typeLabel,
                                modifier = Modifier.size(legIconSize),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = rowText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        // Show the stop name (intermediate destination) after booking details
                        leg.stopName?.takeIf { it.isNotBlank() }?.let { stopName ->
                            Text(
                                text = stopName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp, start = legIconSize + legIconSpacing),
                            )
                        }
                    }
                    // Show total transport duration when first leg has departure and last leg has arrival.
                    val legs = destination.transport?.legs
                    if (!legs.isNullOrEmpty()) {
                        val transportDuration =
                            legs.first().departureDateTime.durationUntil(legs.last().arrivalDateTime)
                        if (transportDuration != null) {
                            Text(
                                text = stringResource(
                                    R.string.itinerary_transport_duration,
                                    if (showTransportDurationInDays) transportDuration.formattedWithDays() else transportDuration.formatted(),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .heightIn(min = 48.dp)
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = stringResource(R.string.itinerary_duration_toggle),
                                        onClick = { showTransportDurationInDays = !showTransportDurationInDays },
                                    ),
                            )
                        }
                    }
                    // "Add destination here" button shown between this item and the next
                    TextButton(
                        onClick = onAddAfter,
                        modifier = Modifier.padding(top = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.itinerary_add_destination_here),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
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
 * A labelled row showing date, time, and timezone buttons for a single [ZonedDateTime] value.
 *
 * - Tapping the date button opens a [DatePickerDialog].
 * - Tapping the time button opens a [TimePicker] dialog (requires a date to be set first).
 * - Tapping the timezone button opens a timezone picker dialog (requires a date to be set first).
 * - Selecting a new date preserves the existing time (or defaults to midnight) and zone.
 * - Changing the timezone keeps the same local date and time, only the zone offset changes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeRow(
    label: String,
    dateTime: ZonedDateTime?,
    onDateTimeChange: (ZonedDateTime?) -> Unit,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
    defaultZoneId: ZoneId = ZoneId.systemDefault(),
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showTimezonePicker by rememberSaveable { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    if (showDatePicker) {
        // Normalize bounds once so an inverted range (e.g. from out-of-order existing data)
        // doesn't disable all dates and lock the user out of correcting them.
        // Both selectableDates and initialDisplayedMonthMillis use these same values.
        val (normalizedMin, normalizedMax) = remember(minDateMillis, maxDateMillis) {
            if (minDateMillis != null && maxDateMillis != null && minDateMillis > maxDateMillis) {
                maxDateMillis to minDateMillis
            } else {
                minDateMillis to maxDateMillis
            }
        }
        val selectableDates = remember(normalizedMin, normalizedMax) {
            val minYear = normalizedMin?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY).year }
            val maxYear = normalizedMax?.let { LocalDate.ofEpochDay(it / MILLIS_PER_DAY).year }
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val afterMin = normalizedMin == null || utcTimeMillis >= normalizedMin
                    val beforeMax = normalizedMax == null || utcTimeMillis <= normalizedMax
                    return afterMin && beforeMax
                }
                override fun isSelectableYear(year: Int): Boolean {
                    val afterMin = minYear == null || year >= minYear
                    val beforeMax = maxYear == null || year <= maxYear
                    return afterMin && beforeMax
                }
            }
        }
        // When no date is selected yet, open the picker at the month of the nearest contextual
        // bound so the user sees a relevant month instead of today, which may be far from the
        // trip dates. normalizedMin is preferred because it represents the chronological
        // predecessor in the itinerary (e.g. the previous destination's departure), giving the
        // user a natural starting point for sequential date entry.
        val initialDisplayedMonthMillis = dateTime.toDateEpochMillis() ?: normalizedMin ?: normalizedMax
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.toDateEpochMillis(),
            initialDisplayedMonthMillis = initialDisplayedMonthMillis,
            selectableDates = selectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val pickedDate = LocalDate.ofEpochDay(millis / MILLIS_PER_DAY)
                            val existingTime = dateTime?.toLocalTime() ?: LocalTime.MIDNIGHT
                            val zone = dateTime?.zone ?: defaultZoneId
                            onDateTimeChange(ZonedDateTime.of(pickedDate, existingTime, zone))
                        }
                        showDatePicker = false
                    },
                ) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val timeState = rememberTimePickerState(
            initialHour = dateTime?.hour ?: 0,
            initialMinute = dateTime?.minute ?: 0,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.itinerary_pick_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // dateTime is always non-null here: the time button is disabled when
                        // dateTime == null, so this branch is purely defensive.
                        val date = dateTime?.toLocalDate() ?: LocalDate.now(defaultZoneId)
                        val zone = dateTime?.zone ?: defaultZoneId
                        onDateTimeChange(ZonedDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute), zone))
                        showTimePicker = false
                    },
                ) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    if (showTimezonePicker) {
        TimezonePickerDialog(
            onTimezoneSelected = { zoneId ->
                val zone = if (zoneId != null) ZoneId.of(zoneId) else ZoneId.systemDefault()
                dateTime?.let { onDateTimeChange(it.withZoneSameLocal(zone)) }
                showTimezonePicker = false
            },
            onDismiss = { showTimezonePicker = false },
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = { showDatePicker = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
        ) {
            Text(
                text = dateTime?.toLocalDate()?.format(dateFormatter)
                    ?: stringResource(R.string.itinerary_pick_date),
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedButton(
            onClick = { showTimePicker = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
            enabled = dateTime != null,
        ) {
            Text(
                text = if (dateTime != null) dateTime.format(timeFormatter) else "--:--",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedButton(
            onClick = { showTimezonePicker = true },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
            enabled = dateTime != null,
        ) {
            Text(
                text = dateTime?.zone?.getDisplayName(TextStyle.SHORT, Locale.getDefault()) ?: "---",
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Empty-state message shown when a trip has no destinations yet. */
@Composable
private fun ItineraryEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.itinerary_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.itinerary_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
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
        title = { Text(stringResource(R.string.itinerary_add_destination_title)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text(stringResource(R.string.itinerary_destination_name_label)) },
                singleLine = true,
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.itinerary_delete_destination_dialog_title)) },
        text = { Text(stringResource(R.string.itinerary_delete_destination_dialog_message, destinationName)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.itinerary_delete_destination_dialog_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
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
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.itinerary_timezone_range_timezone),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(
                        onClick = { showTimezonePicker = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        modifier = Modifier.height(32.dp),
                    ) {
                        Text(
                            text = timezoneButtonLabel,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
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
 * A labelled row with an [OutlinedButton] that opens a [DropdownMenu] listing all destinations.
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
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                contentPadding = PaddingValues(start = 8.dp, end = 4.dp, top = 0.dp, bottom = 0.dp),
                modifier = Modifier.height(32.dp),
            ) {
                Text(
                    text = destinations.getOrNull(selectedIndex)?.name.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
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
                    )
                }
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
