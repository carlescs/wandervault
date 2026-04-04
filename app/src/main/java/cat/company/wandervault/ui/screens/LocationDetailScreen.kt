package cat.company.wandervault.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Activity
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.Transport
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

/** Tabs shown in the Location Detail bottom navigation bar. */
private enum class LocationDetailTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    OVERVIEW(R.string.location_detail_tab_overview, Icons.Default.Info),
    HOTEL(R.string.location_detail_tab_hotel, Icons.Default.Hotel),
    ACTIVITIES(R.string.location_detail_tab_activities, Icons.Default.DirectionsRun),
    NOTES(R.string.location_detail_tab_notes, Icons.Default.Notes),
}

/**
 * Location Detail screen entry point.
 *
 * Loads the destination from the repository by [destinationId] and displays its summary.
 *
 * @param destinationId The ID of the destination whose details are displayed.
 * @param onNavigateUp Called when the user taps the back/up button.
 * @param onTransportClick Called with the destination ID when the user taps the transport row.
 * @param modifier Optional [Modifier].
 */
@Composable
fun LocationDetailScreen(
    destinationId: Int,
    onNavigateUp: () -> Unit,
    onTransportClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: LocationDetailViewModel = koinViewModel(key = "LocationDetailViewModel:$destinationId", parameters = { parametersOf(destinationId) }),
) {
    LaunchedEffect(destinationId) {
        viewModel.loadDestination(destinationId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val saveAndNavigateUp: () -> Unit = {
        viewModel.onSave()
        onNavigateUp()
    }
    // Intercept system back so it follows the same save path as the toolbar up button.
    BackHandler(onBack = saveAndNavigateUp)
    LocationDetailContent(
        uiState = uiState,
        onNavigateUp = saveAndNavigateUp,
        onTransportClick = onTransportClick,
        onHotelNameChange = viewModel::onHotelNameChange,
        onHotelAddressChange = viewModel::onHotelAddressChange,
        onHotelReservationNumberChange = viewModel::onHotelReservationNumberChange,
        onNotesChange = viewModel::onNotesChange,
        onOpenNewActivityDraft = viewModel::onOpenNewActivityDraft,
        onEditActivity = viewModel::onEditActivity,
        onCloseActivityDraft = viewModel::onCloseActivityDraft,
        onActivityDraftTitleChange = viewModel::onActivityDraftTitleChange,
        onActivityDraftDescriptionChange = viewModel::onActivityDraftDescriptionChange,
        onActivityDraftDateTimeChange = viewModel::onActivityDraftDateTimeChange,
        onActivityDraftConfirmationNumberChange = viewModel::onActivityDraftConfirmationNumberChange,
        onSaveActivityDraft = viewModel::onSaveActivityDraft,
        onDeleteActivity = viewModel::onDeleteActivity,
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
    onTransportClick: (Int) -> Unit = {},
    onHotelNameChange: (String) -> Unit = {},
    onHotelAddressChange: (String) -> Unit = {},
    onHotelReservationNumberChange: (String) -> Unit = {},
    onNotesChange: (String) -> Unit = {},
    onOpenNewActivityDraft: () -> Unit = {},
    onEditActivity: (Activity) -> Unit = {},
    onCloseActivityDraft: () -> Unit = {},
    onActivityDraftTitleChange: (String) -> Unit = {},
    onActivityDraftDescriptionChange: (String) -> Unit = {},
    onActivityDraftDateTimeChange: (ZonedDateTime?) -> Unit = {},
    onActivityDraftConfirmationNumberChange: (String) -> Unit = {},
    onSaveActivityDraft: () -> Unit = {},
    onDeleteActivity: (Activity) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val title = when (uiState) {
        is LocationDetailUiState.Success -> uiState.destination.name
        else -> ""
    }
    var selectedTab by rememberSaveable { mutableStateOf(LocationDetailTab.OVERVIEW) }
    val hotelTabVisible = uiState is LocationDetailUiState.Success &&
        (!uiState.isFirst && !uiState.isLast)

    // If the Hotel tab is selected but is no longer visible (destination became first/last),
    // fall back to the Overview tab. Done in a side-effect to avoid mutating state directly
    // during composition, and only when we have a Success state so the selection is not reset
    // during transient loading states.
    LaunchedEffect(uiState) {
        (uiState as? LocationDetailUiState.Success)?.let { successState ->
            if (!successState.isFirst && !successState.isLast) return@let
            if (selectedTab == LocationDetailTab.HOTEL) selectedTab = LocationDetailTab.OVERVIEW
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title) },
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
                    if (tab == LocationDetailTab.HOTEL && !hotelTabVisible) return@forEach
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
        when (uiState) {
            is LocationDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            is LocationDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.location_detail_not_found))
                }
            }
            is LocationDetailUiState.Success -> {
                when (selectedTab) {
                    LocationDetailTab.OVERVIEW -> OverviewTabContent(
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onTransportClick = onTransportClick,
                    )
                    LocationDetailTab.HOTEL -> HotelTabContent(
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onNameChange = onHotelNameChange,
                        onAddressChange = onHotelAddressChange,
                        onReservationNumberChange = onHotelReservationNumberChange,
                    )
                    LocationDetailTab.ACTIVITIES -> ActivitiesTabContent(
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onOpenNewActivityDraft = onOpenNewActivityDraft,
                        onEditActivity = onEditActivity,
                        onCloseActivityDraft = onCloseActivityDraft,
                        onActivityDraftTitleChange = onActivityDraftTitleChange,
                        onActivityDraftDescriptionChange = onActivityDraftDescriptionChange,
                        onActivityDraftDateTimeChange = onActivityDraftDateTimeChange,
                        onActivityDraftConfirmationNumberChange = onActivityDraftConfirmationNumberChange,
                        onSaveActivityDraft = onSaveActivityDraft,
                        onDeleteActivity = onDeleteActivity,
                    )
                    LocationDetailTab.NOTES -> NotesTabContent(
                        uiState = uiState,
                        innerPadding = innerPadding,
                        onNotesChange = onNotesChange,
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTabContent(
    uiState: LocationDetailUiState.Success,
    innerPadding: PaddingValues,
    onTransportClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val destination = uiState.destination
    val arrivalTransport = uiState.arrivalTransport
    val isFirst = uiState.isFirst
    val isLast = uiState.isLast
    val locale = LocalConfiguration.current.locales[0]
    val dateTimeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withLocale(locale)
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = innerPadding,
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (!isFirst) {
                    destination.arrivalDateTime?.let { arrival ->
                        LabeledInfoRow(
                            label = stringResource(R.string.location_detail_arrival),
                            value = arrival.format(dateTimeFormatter),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (arrivalTransport != null) {
                        TransportsInfoSection(
                            label = stringResource(R.string.location_detail_arrival_transport),
                            transport = arrivalTransport,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                if (!isLast) {
                    destination.departureDateTime?.let { departure ->
                        LabeledInfoRow(
                            label = stringResource(R.string.location_detail_departure),
                            value = departure.format(dateTimeFormatter),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    if (destination.transport != null) {
                        TransportsInfoSection(
                            label = stringResource(R.string.location_detail_transport),
                            transport = destination.transport,
                            modifier = Modifier.clickable(role = Role.Button) { onTransportClick(destination.id) },
                        )
                    } else {
                        LabeledInfoRow(
                            label = stringResource(R.string.location_detail_transport),
                            value = stringResource(R.string.transport_none),
                            onClick = { onTransportClick(destination.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HotelTabContent(
    uiState: LocationDetailUiState.Success,
    innerPadding: PaddingValues,
    onNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onReservationNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hotel = uiState.hotelEditState
    val destination = uiState.destination
    val nights = remember(destination.arrivalDateTime, destination.departureDateTime) {
        val arrival = destination.arrivalDateTime
        val departure = destination.departureDateTime
        if (arrival != null && departure != null) {
            val days = ChronoUnit.DAYS.between(arrival.toLocalDate(), departure.toLocalDate())
            if (days >= 0) days else null
        } else {
            null
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        EditableLabel(
            value = hotel.name,
            onValueChange = onNameChange,
            label = stringResource(R.string.hotel_name_label),
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider()
        EditableLabel(
            value = hotel.address,
            onValueChange = onAddressChange,
            label = stringResource(R.string.hotel_address_label),
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider()
        EditableLabel(
            value = hotel.reservationNumber,
            onValueChange = onReservationNumberChange,
            label = stringResource(R.string.hotel_reservation_number_label),
            modifier = Modifier.fillMaxWidth(),
        )
        HorizontalDivider()
        LabeledInfoRow(
            label = stringResource(R.string.hotel_nights_label),
            value = if (nights != null) {
                stringResource(R.string.hotel_nights_value, nights)
            } else {
                stringResource(R.string.hotel_nights_not_available)
            },
        )
    }
}

@Composable
private fun NotesTabContent(
    uiState: LocationDetailUiState.Success,
    innerPadding: PaddingValues,
    onNotesChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        OutlinedTextField(
            value = uiState.notes,
            onValueChange = onNotesChange,
            label = { Text(stringResource(R.string.notes_label)) },
            placeholder = { Text(stringResource(R.string.notes_placeholder)) },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivitiesTabContent(
    uiState: LocationDetailUiState.Success,
    innerPadding: PaddingValues,
    onOpenNewActivityDraft: () -> Unit,
    onEditActivity: (Activity) -> Unit,
    onCloseActivityDraft: () -> Unit,
    onActivityDraftTitleChange: (String) -> Unit,
    onActivityDraftDescriptionChange: (String) -> Unit,
    onActivityDraftDateTimeChange: (ZonedDateTime?) -> Unit,
    onActivityDraftConfirmationNumberChange: (String) -> Unit,
    onSaveActivityDraft: () -> Unit,
    onDeleteActivity: (Activity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = innerPadding,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (uiState.activities.isEmpty() && uiState.activityDraft == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.activities_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(uiState.activities, key = { it.id }) { activity ->
            ActivityCard(
                activity = activity,
                dateFormatter = dateFormatter,
                timeFormatter = timeFormatter,
                onEdit = { onEditActivity(activity) },
                onDelete = { onDeleteActivity(activity) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        // Activity draft form (for adding or editing)
        if (uiState.activityDraft != null) {
            item {
                ActivityDraftForm(
                    draft = uiState.activityDraft,
                    onTitleChange = onActivityDraftTitleChange,
                    onDescriptionChange = onActivityDraftDescriptionChange,
                    onDateTimeChange = onActivityDraftDateTimeChange,
                    onConfirmationNumberChange = onActivityDraftConfirmationNumberChange,
                    onSave = onSaveActivityDraft,
                    onCancel = onCloseActivityDraft,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // "Add activity" button
        if (uiState.activityDraft == null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    OutlinedButton(
                        onClick = onOpenNewActivityDraft,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.activities_add))
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityCard(
    activity: Activity,
    dateFormatter: DateTimeFormatter,
    timeFormatter: DateTimeFormatter,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.padding(0.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.activities_edit),
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.padding(0.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.activities_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
            if (activity.dateTime != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(activity.dateTime.format(dateFormatter))
                        append("  ")
                        append(activity.dateTime.format(timeFormatter))
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (activity.confirmationNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                LabeledInfoRow(
                    label = stringResource(R.string.activity_confirmation_number_label),
                    value = activity.confirmationNumber,
                )
            }
            if (activity.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activity.description,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActivityDraftForm(
    draft: ActivityEditState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDateTimeChange: (ZonedDateTime?) -> Unit,
    onConfirmationNumberChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    }
    val timeFormatter = remember(locale) {
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = draft.dateTime?.toLocalDate()
                ?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val pickedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            val existingTime = draft.dateTime?.toLocalTime() ?: LocalTime.MIDNIGHT
                            val zone = draft.dateTime?.zone ?: ZoneId.systemDefault()
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

    if (showTimePicker && draft.dateTime != null) {
        val timeState = rememberTimePickerState(
            initialHour = draft.dateTime.hour,
            initialMinute = draft.dateTime.minute,
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.activity_time_label)) },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDateTimeChange(
                            ZonedDateTime.of(
                                draft.dateTime.toLocalDate(),
                                LocalTime.of(timeState.hour, timeState.minute),
                                draft.dateTime.zone,
                            ),
                        )
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
    } else if (showTimePicker) {
        // draft.dateTime became null after state restoration – close the dialog.
        showTimePicker = false
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.activity_title_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.description,
                onValueChange = onDescriptionChange,
                label = { Text(stringResource(R.string.activity_description_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
            // Date & time row
            Column {
                Text(
                    text = stringResource(R.string.activity_datetime_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = draft.dateTime?.format(dateFormatter)
                                ?: stringResource(R.string.activity_date_not_set),
                        )
                    }
                    OutlinedButton(
                        onClick = { showTimePicker = true },
                        enabled = draft.dateTime != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = draft.dateTime?.format(timeFormatter)
                                ?: stringResource(R.string.activity_time_not_set),
                        )
                    }
                }
                if (draft.dateTime != null) {
                    TextButton(
                        onClick = { onDateTimeChange(null) },
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        Text(stringResource(R.string.activity_clear_datetime))
                    }
                }
            }
            OutlinedTextField(
                value = draft.confirmationNumber,
                onValueChange = onConfirmationNumberChange,
                label = { Text(stringResource(R.string.activity_confirmation_number_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.activities_cancel))
                }
                Button(
                    onClick = onSave,
                    enabled = draft.title.isNotBlank(),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.activities_save))
                }
            }
        }
    }
}

@Composable
private fun TransportsInfoSection(
    label: String,
    transport: Transport,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        transport.legs.forEachIndexed { index, leg ->
            val legLabel = if (transport.legs.size > 1) {
                stringResource(R.string.transport_leg_label, label, index + 1)
            } else {
                label
            }
            if (index > 0) {
                Spacer(modifier = Modifier.height(8.dp))
            }
            TransportInfoSection(label = legLabel, leg = leg)
        }
    }
}

@Composable
private fun TransportInfoSection(
    label: String,
    leg: TransportLeg,
    modifier: Modifier = Modifier,
) {
    val detailSpacing = 4.dp
    Column(modifier = modifier) {
        LabeledInfoRow(
            label = label,
            value = stringResource(leg.type.labelRes),
        )
        leg.company?.let { company ->
            Spacer(modifier = Modifier.height(detailSpacing))
            LabeledInfoRow(
                label = stringResource(R.string.transport_company_label),
                value = company,
            )
        }
        leg.flightNumber?.let { flightNumber ->
            Spacer(modifier = Modifier.height(detailSpacing))
            LabeledInfoRow(
                label = stringResource(R.string.transport_flight_number_label),
                value = flightNumber,
            )
        }
        leg.reservationConfirmationNumber?.let { confirmation ->
            Spacer(modifier = Modifier.height(detailSpacing))
            LabeledInfoRow(
                label = stringResource(R.string.transport_confirmation_label),
                value = confirmation,
            )
        }
    }
}

@Composable
private fun LabeledInfoRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val paddedModifier = modifier.padding(vertical = 4.dp)
    Column(
        modifier = if (onClick != null) {
            paddedModifier.clickable(role = Role.Button, onClick = onClick)
        } else {
            paddedModifier
        },
    ) {
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
private fun LocationDetailIntermediatePreview() {
    val destination = Destination(
        id = 2,
        tripId = 1,
        name = "Paris",
        position = 1,
        arrivalDateTime = ZonedDateTime.of(2024, 6, 1, 12, 30, 0, 0, ZoneId.of("Europe/Paris")),
        departureDateTime = ZonedDateTime.of(2024, 6, 3, 10, 0, 0, 0, ZoneId.of("Europe/Paris")),
        transport = Transport(
            id = 2,
            destinationId = 2,
            legs = listOf(
                TransportLeg(transportId = 2, type = TransportType.TRAIN, company = "Eurostar", reservationConfirmationNumber = "ES9024"),
            ),
        ),
    )
    val arrivalTransport = Transport(
        id = 1,
        destinationId = 1,
        legs = listOf(
            TransportLeg(transportId = 1, type = TransportType.FLIGHT, company = "Air France", flightNumber = "AF1234"),
        ),
    )
    WanderVaultTheme {
        LocationDetailContent(
            uiState = LocationDetailUiState.Success(
                destination = destination,
                arrivalTransport = arrivalTransport,
                isFirst = false,
                isLast = false,
            ),
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationDetailFirstStopPreview() {
    val destination = Destination(
        id = 1,
        tripId = 1,
        name = "London",
        position = 0,
        departureDateTime = ZonedDateTime.of(2024, 6, 1, 9, 0, 0, 0, ZoneId.of("Europe/London")),
        transport = Transport(
            id = 1,
            destinationId = 1,
            legs = listOf(
                TransportLeg(transportId = 1, type = TransportType.FLIGHT, company = "Air France", flightNumber = "AF1234"),
            ),
        ),
    )
    WanderVaultTheme {
        LocationDetailContent(
            uiState = LocationDetailUiState.Success(
                destination = destination,
                arrivalTransport = null,
                isFirst = true,
                isLast = false,
            ),
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationDetailLastStopPreview() {
    val destination = Destination(
        id = 3,
        tripId = 1,
        name = "Rome",
        position = 2,
        arrivalDateTime = ZonedDateTime.of(2024, 6, 3, 14, 0, 0, 0, ZoneId.of("Europe/Rome")),
    )
    val arrivalTransport = Transport(
        id = 2,
        destinationId = 2,
        legs = listOf(
            TransportLeg(transportId = 2, type = TransportType.TRAIN, company = "Trenitalia", reservationConfirmationNumber = "FR9302"),
        ),
    )
    WanderVaultTheme {
        LocationDetailContent(
            uiState = LocationDetailUiState.Success(
                destination = destination,
                arrivalTransport = arrivalTransport,
                isFirst = false,
                isLast = true,
            ),
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationDetailNoDatesPreview() {
    val destination = Destination(id = 2, tripId = 1, name = "Rome", position = 1)
    WanderVaultTheme {
        LocationDetailContent(
            uiState = LocationDetailUiState.Success(destination = destination),
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationDetailLoadingPreview() {
    WanderVaultTheme {
        LocationDetailContent(
            uiState = LocationDetailUiState.Loading,
            onNavigateUp = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationDetailHotelTabPreview() {
    val destination = Destination(
        id = 2,
        tripId = 1,
        name = "Paris",
        position = 1,
        arrivalDateTime = ZonedDateTime.of(2024, 6, 1, 12, 30, 0, 0, ZoneId.of("Europe/Paris")),
        departureDateTime = ZonedDateTime.of(2024, 6, 4, 10, 0, 0, 0, ZoneId.of("Europe/Paris")),
    )
    WanderVaultTheme {
        HotelTabContent(
            uiState = LocationDetailUiState.Success(
                destination = destination,
                hotelEditState = HotelEditState(
                    name = "Hotel de Ville",
                    address = "1 Rue de Rivoli, Paris",
                    reservationNumber = "HV-20240601",
                ),
            ),
            innerPadding = PaddingValues(0.dp),
            onNameChange = {},
            onAddressChange = {},
            onReservationNumberChange = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LocationDetailActivitiesTabPreview() {
    val destination = Destination(
        id = 2,
        tripId = 1,
        name = "Paris",
        position = 1,
        arrivalDateTime = ZonedDateTime.of(2024, 6, 1, 12, 30, 0, 0, ZoneId.of("Europe/Paris")),
        departureDateTime = ZonedDateTime.of(2024, 6, 4, 10, 0, 0, 0, ZoneId.of("Europe/Paris")),
    )
    val activities = listOf(
        Activity(
            id = 1,
            destinationId = 2,
            title = "Eiffel Tower Visit",
            description = "Skip-the-line ticket",
            dateTime = ZonedDateTime.of(2024, 6, 2, 10, 0, 0, 0, ZoneId.of("Europe/Paris")),
            confirmationNumber = "ET-123456",
        ),
    )
    WanderVaultTheme {
        ActivitiesTabContent(
            uiState = LocationDetailUiState.Success(
                destination = destination,
                activities = activities,
            ),
            innerPadding = PaddingValues(0.dp),
            onOpenNewActivityDraft = {},
            onEditActivity = {},
            onCloseActivityDraft = {},
            onActivityDraftTitleChange = {},
            onActivityDraftDescriptionChange = {},
            onActivityDraftDateTimeChange = {},
            onActivityDraftConfirmationNumberChange = {},
            onSaveActivityDraft = {},
            onDeleteActivity = {},
        )
    }
}
