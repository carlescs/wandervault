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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MILLIS_PER_DAY = 86_400_000L

/** Converts a [LocalDateTime] to epoch-day milliseconds used by the [DatePicker] API. */
private fun LocalDateTime?.toDateEpochMillis(): Long? =
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
    viewModel: ItineraryViewModel = koinViewModel(parameters = { parametersOf(tripId) }),
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
        onUpdateTransport = viewModel::onUpdateTransport,
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
    onUpdateArrivalDateTime: (Destination, LocalDateTime?) -> Unit,
    onUpdateDepartureDateTime: (Destination, LocalDateTime?) -> Unit,
    onDeleteDestination: (Destination) -> Unit,
    onDismissDeleteDestinationDialog: () -> Unit,
    onConfirmDeleteDestination: () -> Unit,
    onMoveDestinationUp: (Destination) -> Unit,
    onMoveDestinationDown: (Destination) -> Unit,
    onAddDestinationAfter: (Int) -> Unit,
    onUpdateTransport: (Destination, Transport?) -> Unit,
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
                        onSelectTransport = { transport -> onUpdateTransport(destination, transport) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onAddDestinationClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 16.dp,
                ),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.itinerary_add_destination))
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
}

@Composable
private fun DestinationTimelineItem(
    destination: Destination,
    isFirst: Boolean,
    isLast: Boolean,
    previousDestination: Destination?,
    nextDestination: Destination?,
    onUpdateArrivalDateTime: (LocalDateTime?) -> Unit,
    onUpdateDepartureDateTime: (LocalDateTime?) -> Unit,
    onDeleteDestination: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onAddAfter: () -> Unit,
    onSelectTransport: (Transport?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showTransportPicker by rememberSaveable { mutableStateOf(false) }

    if (showTransportPicker) {
        TransportPickerDialog(
            currentTransport = destination.transport,
            onSelect = { transport ->
                onSelectTransport(transport)
                showTransportPicker = false
            },
            onDismiss = { showTransportPicker = false },
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
    ) {
        // Timeline column: line → circle → line (with optional transport icon)
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
                // Bottom line with transport circle overlaid at the centre
                val hasTransport = destination.transport != null
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    // Transport circle: filled when a transport is set, dimmed outline otherwise
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
                                onClick = { showTransportPicker = true },
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = destination.transport?.type?.icon ?: Icons.Default.Add,
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
            }
        }

        // Content column
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
                    modifier = Modifier.weight(1f),
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

            val prevDepartureMillis = previousDestination?.departureDateTime.toDateEpochMillis()
            val nextArrivalMillis = nextDestination?.arrivalDateTime.toDateEpochMillis()
            val ownArrivalMillis = destination.arrivalDateTime.toDateEpochMillis()
            val ownDepartureMillis = destination.departureDateTime.toDateEpochMillis()
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

            if (!isLast) {
                // Show transport details (company, flight/reference number, confirmation) when set
                val transport = destination.transport
                if (transport != null) {
                    val details = listOfNotNull(
                        transport.company,
                        transport.flightNumber,
                        transport.reservationConfirmationNumber,
                    )
                    if (details.isNotEmpty()) {
                        Text(
                            text = details.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
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
 * Dialog for choosing (or clearing) the [TransportType] and entering transport details
 * (company, flight/reference number, confirmation number) for a destination leg.
 */
@Composable
private fun TransportPickerDialog(
    currentTransport: Transport?,
    onSelect: (Transport?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedType by rememberSaveable { mutableStateOf(currentTransport?.type) }
    var company by rememberSaveable { mutableStateOf(currentTransport?.company ?: "") }
    var flightNumber by rememberSaveable { mutableStateOf(currentTransport?.flightNumber ?: "") }
    var confirmationNumber by rememberSaveable {
        mutableStateOf(currentTransport?.reservationConfirmationNumber ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.itinerary_transport_picker_title)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                // Render transport options in rows of 4
                TransportType.entries.chunked(4).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        rowItems.forEach { type ->
                            TransportOption(
                                icon = type.icon,
                                label = stringResource(type.labelRes),
                                isSelected = type == selectedType,
                                onClick = { selectedType = type },
                            )
                        }
                    }
                }
                // "None" option to clear the transport
                Row(modifier = Modifier.fillMaxWidth()) {
                    TransportOption(
                        icon = Icons.Default.Close,
                        label = stringResource(R.string.transport_none),
                        isSelected = selectedType == null,
                        onClick = { selectedType = null },
                    )
                }
                // Detail fields shown when a transport type is selected
                if (selectedType != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text(stringResource(R.string.transport_company_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = flightNumber,
                        onValueChange = { flightNumber = it },
                        label = { Text(stringResource(R.string.transport_flight_number_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = confirmationNumber,
                        onValueChange = { confirmationNumber = it },
                        label = { Text(stringResource(R.string.transport_confirmation_label)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val type = selectedType
                    val transport = if (type != null) {
                        Transport(
                            id = currentTransport?.id ?: 0,
                            destinationId = currentTransport?.destinationId ?: 0,
                            type = type,
                            company = company.trim().takeIf { it.isNotBlank() },
                            flightNumber = flightNumber.trim().takeIf { it.isNotBlank() },
                            reservationConfirmationNumber = confirmationNumber.trim().takeIf { it.isNotBlank() },
                        )
                    } else {
                        null
                    }
                    onSelect(transport)
                },
            ) { Text(stringResource(R.string.dialog_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun TransportOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
        )
    }
}

/**
 * A labelled row showing date and time buttons for a single [LocalDateTime] value.
 *
 * - Tapping the date button opens a [DatePickerDialog].
 * - Tapping the time button opens a [TimePicker] dialog (requires a date to be set first).
 * - Selecting a new date preserves the existing time (or defaults to midnight).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimeRow(
    label: String,
    dateTime: LocalDateTime?,
    onDateTimeChange: (LocalDateTime?) -> Unit,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

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
                            onDateTimeChange(LocalDateTime.of(pickedDate, existingTime))
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
                        val date = dateTime?.toLocalDate() ?: LocalDate.now()
                        onDateTimeChange(LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute)))
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
                text = dateTime?.format(timeFormatter) ?: "--:--",
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

// ── Previews ─────────────────────────────────────────────────────────────────

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
            onUpdateTransport = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItineraryWithDestinationsPreview() {
    val destinations = listOf(
        Destination(1, 1, "London", 0, departureDateTime = LocalDateTime.of(2024, 6, 1, 9, 0), transport = Transport(destinationId = 1, type = TransportType.FLIGHT)),
        Destination(
            2,
            1,
            "Paris",
            1,
            arrivalDateTime = LocalDateTime.of(2024, 6, 1, 12, 30),
            departureDateTime = LocalDateTime.of(2024, 6, 3, 10, 0),
            transport = Transport(destinationId = 2, type = TransportType.TRAIN),
        ),
        Destination(3, 1, "Rome", 2, arrivalDateTime = LocalDateTime.of(2024, 6, 3, 14, 0)),
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
            onUpdateTransport = { _, _ -> },
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
            onUpdateTransport = { _, _ -> },
        )
    }
}
