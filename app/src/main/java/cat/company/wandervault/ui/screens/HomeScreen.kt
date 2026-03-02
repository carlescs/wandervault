package cat.company.wandervault.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Home screen – displays the user's list of trips.
 *
 * When no trips exist an empty-state message is shown.
 * A FAB opens the [AddTripDialog] to create a new trip.
 */
@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        uiState = uiState,
        onAddTripClick = viewModel::onAddTripClick,
        onDismissDialog = viewModel::onDismissAddTripDialog,
        onTitleChange = viewModel::onAddTripTitleChange,
        onStartDateChange = viewModel::onAddTripStartDateChange,
        onEndDateChange = viewModel::onAddTripEndDateChange,
        onSaveTrip = viewModel::onSaveTrip,
        onEditTripClick = viewModel::onEditTripClick,
        onDismissEditDialog = viewModel::onDismissEditTripDialog,
        onEditTitleChange = viewModel::onEditTripTitleChange,
        onEditStartDateChange = viewModel::onEditTripStartDateChange,
        onEditEndDateChange = viewModel::onEditTripEndDateChange,
        onUpdateTrip = viewModel::onUpdateTrip,
        modifier = modifier,
    )
}

/**
 * Stateless presentation of the home screen.
 *
 * Accepts a [HomeUiState] snapshot and event callbacks so it can be reused
 * in `@Preview` functions without a real [HomeViewModel].
 */
@Composable
internal fun HomeScreenContent(
    uiState: HomeUiState,
    onAddTripClick: () -> Unit,
    onDismissDialog: () -> Unit,
    onTitleChange: (String) -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onSaveTrip: () -> Unit,
    onEditTripClick: (Trip) -> Unit,
    onDismissEditDialog: () -> Unit,
    onEditTitleChange: (String) -> Unit,
    onEditStartDateChange: (LocalDate) -> Unit,
    onEditEndDateChange: (LocalDate) -> Unit,
    onUpdateTrip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        if (uiState.trips.isEmpty()) {
            TripsEmptyState(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.trips) { trip ->
                    TripCard(trip = trip, onEditClick = { onEditTripClick(trip) })
                }
            }
        }

        FloatingActionButton(
            onClick = onAddTripClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_trip_content_desc))
        }
    }

    if (uiState.showAddTripDialog) {
        AddTripDialog(
            title = uiState.addTripTitle,
            onTitleChange = onTitleChange,
            startDate = uiState.addTripStartDate,
            onStartDateChange = onStartDateChange,
            endDate = uiState.addTripEndDate,
            onEndDateChange = onEndDateChange,
            isFormValid = uiState.isAddTripFormValid,
            onSave = onSaveTrip,
            onDismiss = onDismissDialog,
        )
    }

    if (uiState.showEditTripDialog) {
        EditTripDialog(
            title = uiState.editTripTitle,
            onTitleChange = onEditTitleChange,
            startDate = uiState.editTripStartDate,
            onStartDateChange = onEditStartDateChange,
            endDate = uiState.editTripEndDate,
            onEndDateChange = onEditEndDateChange,
            isFormValid = uiState.isEditTripFormValid,
            onSave = onUpdateTrip,
            onDismiss = onDismissEditDialog,
        )
    }
}

@Composable
private fun TripCard(trip: Trip, onEditClick: () -> Unit, modifier: Modifier = Modifier) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = trip.title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${trip.startDate.format(formatter)} – ${trip.endDate.format(formatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.edit_trip_content_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * Shared dialog for creating or editing a trip.
 *
 * Used by [AddTripDialog] and [EditTripDialog] to avoid duplication.
 *
 * @param dialogTitle The title shown at the top of the dialog.
 * @param title The current trip name input value.
 * @param onTitleChange Called when the user changes the trip name.
 * @param startDate The currently selected start date, or null if none chosen yet.
 * @param onStartDateChange Called when the user picks a new start date.
 * @param endDate The currently selected end date, or null if none chosen yet.
 * @param onEndDateChange Called when the user picks a new end date.
 * @param isFormValid Whether the form inputs are valid, enabling the save button.
 * @param onSave Called when the user confirms the dialog.
 * @param onDismiss Called when the user cancels or dismisses the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TripFormDialog(
    dialogTitle: String,
    title: String,
    onTitleChange: (String) -> Unit,
    startDate: LocalDate?,
    onStartDateChange: (LocalDate) -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate) -> Unit,
    isFormValid: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    if (showStartDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = startDate?.toEpochDay()?.times(MILLIS_PER_DAY),
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onStartDateChange(LocalDate.ofEpochDay(millis / MILLIS_PER_DAY))
                    }
                    showStartDatePicker = false
                }) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        ) { DatePicker(state = state) }
    }

    if (showEndDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = endDate?.toEpochDay()?.times(MILLIS_PER_DAY),
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        onEndDateChange(LocalDate.ofEpochDay(millis / MILLIS_PER_DAY))
                    }
                    showEndDatePicker = false
                }) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        ) { DatePicker(state = state) }
    }

    val formatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text(stringResource(R.string.add_trip_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = startDate?.format(formatter) ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.add_trip_start_date_label)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showStartDatePicker = true }) {
                            Text(stringResource(R.string.add_trip_pick_date))
                        }
                    },
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = endDate?.format(formatter) ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.add_trip_end_date_label)) },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { showEndDatePicker = true }) {
                            Text(stringResource(R.string.add_trip_pick_date))
                        }
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = isFormValid) { Text(stringResource(R.string.add_trip_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

@Composable
private fun AddTripDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    startDate: LocalDate?,
    onStartDateChange: (LocalDate) -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate) -> Unit,
    isFormValid: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    TripFormDialog(
        dialogTitle = stringResource(R.string.add_trip_title),
        title = title,
        onTitleChange = onTitleChange,
        startDate = startDate,
        onStartDateChange = onStartDateChange,
        endDate = endDate,
        onEndDateChange = onEndDateChange,
        isFormValid = isFormValid,
        onSave = onSave,
        onDismiss = onDismiss,
    )
}

@Composable
private fun EditTripDialog(
    title: String,
    onTitleChange: (String) -> Unit,
    startDate: LocalDate?,
    onStartDateChange: (LocalDate) -> Unit,
    endDate: LocalDate?,
    onEndDateChange: (LocalDate) -> Unit,
    isFormValid: Boolean,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    TripFormDialog(
        dialogTitle = stringResource(R.string.edit_trip_title),
        title = title,
        onTitleChange = onTitleChange,
        startDate = startDate,
        onStartDateChange = onStartDateChange,
        endDate = endDate,
        onEndDateChange = onEndDateChange,
        isFormValid = isFormValid,
        onSave = onSave,
        onDismiss = onDismiss,
    )
}

@Composable
private fun TripsEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.trips_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.trips_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenEmptyPreview() {
    WanderVaultTheme {
        HomeScreenContent(
            uiState = HomeUiState(),
            onAddTripClick = {},
            onDismissDialog = {},
            onTitleChange = {},
            onStartDateChange = {},
            onEndDateChange = {},
            onSaveTrip = {},
            onEditTripClick = {},
            onDismissEditDialog = {},
            onEditTitleChange = {},
            onEditStartDateChange = {},
            onEditEndDateChange = {},
            onUpdateTrip = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenWithTripsPreview() {
    val sampleTrips = listOf(
        Trip(1, "Paris Getaway", LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 10)),
        Trip(2, "Tokyo Adventure", LocalDate.of(2024, 9, 15), LocalDate.of(2024, 9, 25)),
    )
    WanderVaultTheme {
        HomeScreenContent(
            uiState = HomeUiState(trips = sampleTrips),
            onAddTripClick = {},
            onDismissDialog = {},
            onTitleChange = {},
            onStartDateChange = {},
            onEndDateChange = {},
            onSaveTrip = {},
            onEditTripClick = {},
            onDismissEditDialog = {},
            onEditTitleChange = {},
            onEditStartDateChange = {},
            onEditEndDateChange = {},
            onUpdateTrip = {},
        )
    }
}

