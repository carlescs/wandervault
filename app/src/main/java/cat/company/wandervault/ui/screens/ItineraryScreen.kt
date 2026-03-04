package cat.company.wandervault.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Destination
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
        onMoveDestinationUp = viewModel::onMoveDestinationUp,
        onMoveDestinationDown = viewModel::onMoveDestinationDown,
        onAddDestinationAfter = { pos -> viewModel.onAddDestinationClick(pos) },
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
    onMoveDestinationUp: (Destination) -> Unit,
    onMoveDestinationDown: (Destination) -> Unit,
    onAddDestinationAfter: (Int) -> Unit,
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
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
    ) {
        // Timeline column: line → circle → line
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

            val isSingle = isFirst && isLast
            val prevDepartureMillis = previousDestination?.departureDateTime.toDateEpochMillis()
            val nextArrivalMillis = nextDestination?.arrivalDateTime.toDateEpochMillis()
            val ownArrivalMillis = destination.arrivalDateTime.toDateEpochMillis()
            val ownDepartureMillis = destination.departureDateTime.toDateEpochMillis()
            // Show arrival for: all non-first destinations, AND the single-destination case
            if (!isFirst || isSingle) {
                DateTimeRow(
                    label = stringResource(R.string.itinerary_arrival_label),
                    dateTime = destination.arrivalDateTime,
                    onDateTimeChange = onUpdateArrivalDateTime,
                    minDateMillis = prevDepartureMillis,
                    maxDateMillis = ownDepartureMillis,
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            // Show departure for: all non-last destinations, AND the single-destination case
            if (!isLast || isSingle) {
                DateTimeRow(
                    label = stringResource(R.string.itinerary_departure_label),
                    dateTime = destination.departureDateTime,
                    onDateTimeChange = onUpdateDepartureDateTime,
                    minDateMillis = listOfNotNull(ownArrivalMillis, prevDepartureMillis).maxOrNull(),
                    maxDateMillis = nextArrivalMillis,
                )
            }

            if (!isLast) {
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
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    if (showDatePicker) {
        val selectableDates = remember(minDateMillis, maxDateMillis) {
            // Normalize bounds so an inverted range (e.g. from out-of-order existing data)
            // doesn't disable all dates and lock the user out of correcting them.
            val (normalizedMin, normalizedMax) =
                if (minDateMillis != null && maxDateMillis != null && minDateMillis > maxDateMillis) {
                    maxDateMillis to minDateMillis
                } else {
                    minDateMillis to maxDateMillis
                }
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val afterMin = normalizedMin == null || utcTimeMillis >= normalizedMin
                    val beforeMax = normalizedMax == null || utcTimeMillis <= normalizedMax
                    return afterMin && beforeMax
                }
                override fun isSelectableYear(year: Int): Boolean = true
            }
        }
        val state = rememberDatePickerState(
            initialSelectedDateMillis = dateTime.toDateEpochMillis(),
            selectableDates = selectableDates,
        )
        DatePickerDialog(
            onDismissRequest = { },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val pickedDate = LocalDate.ofEpochDay(millis / MILLIS_PER_DAY)
                            val existingTime = dateTime?.toLocalTime() ?: LocalTime.MIDNIGHT
                            onDateTimeChange(LocalDateTime.of(pickedDate, existingTime))
                        }
                    },
                ) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { }) {
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
            onDismissRequest = { },
            title = { Text(stringResource(R.string.itinerary_pick_time)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        // dateTime is always non-null here: the time button is disabled when
                        // dateTime == null, so this branch is purely defensive.
                        val date = dateTime?.toLocalDate() ?: LocalDate.now()
                        onDateTimeChange(LocalDateTime.of(date, LocalTime.of(timeState.hour, timeState.minute)))
                    },
                ) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp),
        )
        OutlinedButton(
            onClick = { },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
        ) {
            Text(
                text = dateTime?.toLocalDate()?.format(dateFormatter)
                    ?: stringResource(R.string.itinerary_pick_date),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        OutlinedButton(
            onClick = { },
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
            modifier = Modifier.height(32.dp),
            enabled = dateTime != null,
        ) {
            Text(
                text = dateTime?.format(timeFormatter) ?: "--:--",
                style = MaterialTheme.typography.bodySmall,
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
            onMoveDestinationUp = {},
            onMoveDestinationDown = {},
            onAddDestinationAfter = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItineraryWithDestinationsPreview() {
    val destinations = listOf(
        Destination(1, 1, "London", 0, departureDateTime = LocalDateTime.of(2024, 6, 1, 9, 0)),
        Destination(
            2,
            1,
            "Paris",
            1,
            arrivalDateTime = LocalDateTime.of(2024, 6, 1, 12, 30),
            departureDateTime = LocalDateTime.of(2024, 6, 3, 10, 0),
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
            onMoveDestinationUp = {},
            onMoveDestinationDown = {},
            onAddDestinationAfter = {},
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
            onMoveDestinationUp = {},
            onMoveDestinationDown = {},
            onAddDestinationAfter = {},
        )
    }
}
