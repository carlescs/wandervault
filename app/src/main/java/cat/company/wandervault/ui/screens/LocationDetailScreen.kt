package cat.company.wandervault.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private const val MILLIS_PER_DAY = 86_400_000L

/** Converts a [LocalDate] to epoch-day milliseconds used by the [DatePicker] API. */
private fun LocalDate?.toEpochMillis(): Long? = this?.toEpochDay()?.times(MILLIS_PER_DAY)

/** Tabs shown in the Location Detail bottom navigation bar. */
private enum class LocationDetailTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    SUMMARY(R.string.location_detail_tab_summary, Icons.Default.Info),
    HOTEL(R.string.location_detail_tab_hotel, Icons.Default.Home),
}

/**
 * Location Detail screen entry point.
 *
 * Loads hotel state reactively via [LocationDetailViewModel] and renders destination details
 * and hotel reservation form in a tabbed layout.
 *
 * @param destination The destination whose details are displayed.
 * @param onNavigateUp Called when the user taps the back/up button.
 * @param modifier Optional [Modifier].
 */
@Composable
fun LocationDetailScreen(
    destination: Destination,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LocationDetailViewModel = koinViewModel(parameters = { parametersOf(destination) }),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LocationDetailContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onHotelNameChange = viewModel::onHotelNameChange,
        onHotelAddressChange = viewModel::onHotelAddressChange,
        onHotelCheckInDateChange = viewModel::onHotelCheckInDateChange,
        onHotelCheckOutDateChange = viewModel::onHotelCheckOutDateChange,
        onHotelConfirmationNumberChange = viewModel::onHotelConfirmationNumberChange,
        onHotelNotesChange = viewModel::onHotelNotesChange,
        onSaveHotel = viewModel::onSaveHotel,
        onDeleteHotel = viewModel::onDeleteHotel,
        modifier = modifier,
    )
}

/**
 * Stateless presentation of the Location Detail screen.
 *
 * Accepts a [LocationDetailUiState] snapshot so it can be used in `@Preview` without a real ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LocationDetailContent(
    uiState: LocationDetailUiState,
    onNavigateUp: () -> Unit,
    onHotelNameChange: (String) -> Unit,
    onHotelAddressChange: (String) -> Unit,
    onHotelCheckInDateChange: (LocalDate?) -> Unit,
    onHotelCheckOutDateChange: (LocalDate?) -> Unit,
    onHotelConfirmationNumberChange: (String) -> Unit,
    onHotelNotesChange: (String) -> Unit,
    onSaveHotel: () -> Unit,
    onDeleteHotel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(LocationDetailTab.SUMMARY) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(uiState.destination.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.location_detail_navigate_up),
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                LocationDetailTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        when (selectedTab) {
            LocationDetailTab.SUMMARY -> SummaryTabContent(uiState = uiState, innerPadding = innerPadding)
            LocationDetailTab.HOTEL -> HotelTabContent(
                uiState = uiState,
                innerPadding = innerPadding,
                onHotelNameChange = onHotelNameChange,
                onHotelAddressChange = onHotelAddressChange,
                onHotelCheckInDateChange = onHotelCheckInDateChange,
                onHotelCheckOutDateChange = onHotelCheckOutDateChange,
                onHotelConfirmationNumberChange = onHotelConfirmationNumberChange,
                onHotelNotesChange = onHotelNotesChange,
                onSaveHotel = onSaveHotel,
                onDeleteHotel = onDeleteHotel,
            )
        }
    }
}

@Composable
private fun SummaryTabContent(
    uiState: LocationDetailUiState,
    innerPadding: PaddingValues,
) {
    val dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = innerPadding,
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = uiState.destination.name,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                uiState.destination.arrivalDateTime?.let { arrival ->
                    LabeledInfoRow(
                        label = stringResource(R.string.location_detail_arrival),
                        value = arrival.format(dateTimeFormatter),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                uiState.destination.departureDateTime?.let { departure ->
                    LabeledInfoRow(
                        label = stringResource(R.string.location_detail_departure),
                        value = departure.format(dateTimeFormatter),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                uiState.destination.transport?.let { transport ->
                    LabeledInfoRow(
                        label = stringResource(R.string.location_detail_transport),
                        value = stringResource(transport.type.labelRes),
                    )
                }
            }
        }
    }
}

@Composable
private fun LabeledInfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HotelTabContent(
    uiState: LocationDetailUiState,
    innerPadding: PaddingValues,
    onHotelNameChange: (String) -> Unit,
    onHotelAddressChange: (String) -> Unit,
    onHotelCheckInDateChange: (LocalDate?) -> Unit,
    onHotelCheckOutDateChange: (LocalDate?) -> Unit,
    onHotelConfirmationNumberChange: (String) -> Unit,
    onHotelNotesChange: (String) -> Unit,
    onSaveHotel: () -> Unit,
    onDeleteHotel: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = uiState.hotelName,
                    onValueChange = onHotelNameChange,
                    label = { Text(stringResource(R.string.hotel_name_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.hotelAddress,
                    onValueChange = onHotelAddressChange,
                    label = { Text(stringResource(R.string.hotel_address_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                DatePickerRow(
                    label = stringResource(R.string.hotel_check_in_label),
                    date = uiState.hotelCheckInDate,
                    onDateChange = onHotelCheckInDateChange,
                )
                DatePickerRow(
                    label = stringResource(R.string.hotel_check_out_label),
                    date = uiState.hotelCheckOutDate,
                    onDateChange = onHotelCheckOutDateChange,
                )
                OutlinedTextField(
                    value = uiState.hotelConfirmationNumber,
                    onValueChange = onHotelConfirmationNumberChange,
                    label = { Text(stringResource(R.string.hotel_confirmation_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = uiState.hotelNotes,
                    onValueChange = onHotelNotesChange,
                    label = { Text(stringResource(R.string.hotel_notes_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                )
                Button(
                    onClick = onSaveHotel,
                    enabled = uiState.hotelName.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.hotel_save))
                }
                if (uiState.hotel != null) {
                    OutlinedButton(
                        onClick = onDeleteHotel,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.hotel_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A labelled row with a button that opens a [DatePickerDialog] to pick a [LocalDate].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerRow(
    label: String,
    date: LocalDate?,
    onDateChange: (LocalDate?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

    if (showPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.toEpochMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            onDateChange(LocalDate.ofEpochDay(millis / MILLIS_PER_DAY))
                        }
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.dialog_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        ) { DatePicker(state = state) }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .padding(top = 4.dp),
        )
        OutlinedButton(onClick = { showPicker = true }) {
            Text(
                text = date?.format(dateFormatter) ?: stringResource(R.string.hotel_pick_date),
            )
        }
    }
}

private val TransportType.labelRes: Int
    @StringRes get() = when (this) {
        TransportType.WALKING -> R.string.transport_walking
        TransportType.CYCLING -> R.string.transport_cycling
        TransportType.DRIVING -> R.string.transport_driving
        TransportType.BUS -> R.string.transport_bus
        TransportType.TRAIN -> R.string.transport_train
        TransportType.FERRY -> R.string.transport_ferry
        TransportType.FLIGHT -> R.string.transport_flight
        TransportType.OTHER -> R.string.transport_other
    }

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun LocationDetailSummaryPreview() {
    val destination = Destination(
        id = 1,
        tripId = 1,
        name = "Paris",
        position = 0,
        arrivalDateTime = LocalDateTime.of(2024, 6, 3, 10, 30),
        departureDateTime = LocalDateTime.of(2024, 6, 7, 14, 0),
        transport = Transport(destinationId = 1, type = TransportType.FLIGHT),
    )
    WanderVaultTheme {
        LocationDetailContent(
            uiState = LocationDetailUiState(destination = destination, isLoading = false),
            onNavigateUp = {},
            onHotelNameChange = {},
            onHotelAddressChange = {},
            onHotelCheckInDateChange = {},
            onHotelCheckOutDateChange = {},
            onHotelConfirmationNumberChange = {},
            onHotelNotesChange = {},
            onSaveHotel = {},
            onDeleteHotel = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationDetailHotelPreview() {
    val destination = Destination(id = 2, tripId = 1, name = "Rome", position = 1)
    val hotel = Hotel(
        id = 1,
        destinationId = 2,
        name = "Hotel Colosseo",
        address = "Via Sacra 1, Rome",
        checkInDate = LocalDate.of(2024, 6, 8),
        checkOutDate = LocalDate.of(2024, 6, 12),
        confirmationNumber = "ABC123",
        notes = "Breakfast included.",
    )
    WanderVaultTheme {
        LocationDetailContent(
            uiState = LocationDetailUiState(
                destination = destination,
                hotel = hotel,
                isLoading = false,
                hotelName = hotel.name,
                hotelAddress = hotel.address,
                hotelCheckInDate = hotel.checkInDate,
                hotelCheckOutDate = hotel.checkOutDate,
                hotelConfirmationNumber = hotel.confirmationNumber,
                hotelNotes = hotel.notes,
            ),
            onNavigateUp = {},
            onHotelNameChange = {},
            onHotelAddressChange = {},
            onHotelCheckInDateChange = {},
            onHotelCheckOutDateChange = {},
            onHotelConfirmationNumberChange = {},
            onHotelNotesChange = {},
            onSaveHotel = {},
            onDeleteHotel = {},
        )
    }
}
