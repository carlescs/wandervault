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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import java.time.Duration
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

/** Tabs shown in the Transport Detail bottom navigation bar. */
private enum class TransportDetailTab(@StringRes val labelRes: Int, val icon: ImageVector) {
    DETAILS(R.string.transport_detail_tab_details, Icons.Default.Info),
    LEGS(R.string.transport_detail_tab_legs, Icons.Default.AltRoute),
}

/**
 * Transport Detail screen entry point.
 *
 * Loads the destination from the repository by [destinationId] and allows inline editing of its
 * transport legs. Multiple legs can be added, edited, and removed.
 *
 * @param destinationId The ID of the destination whose transport legs are displayed and edited.
 * @param onNavigateUp Called when the user taps the back/up button.
 * @param modifier Optional [Modifier].
 */
@Composable
fun TransportDetailScreen(
    destinationId: Int,
    onNavigateUp: () -> Unit,
    onNavigateToDocument: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: TransportDetailViewModel = koinViewModel(
        key = "TransportDetailViewModel:$destinationId",
    ),
) {
    LaunchedEffect(destinationId) {
        viewModel.loadDestination(destinationId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // Intercept system back so it follows the same save path as the toolbar up button.
    BackHandler {
        viewModel.onSave()
        onNavigateUp()
    }
    TransportDetailContent(
        uiState = uiState,
        onNavigateUp = {
            viewModel.onSave()
            onNavigateUp()
        },
        onAddLeg = viewModel::onAddLeg,
        onRemoveLeg = viewModel::onRemoveLeg,
        onMoveLegUp = viewModel::onMoveLegUp,
        onMoveLegDown = viewModel::onMoveLegDown,
        onTypeSelected = viewModel::onTypeSelected,
        onStopNameChange = viewModel::onStopNameChange,
        onCompanyChange = viewModel::onCompanyChange,
        onFlightNumberChange = viewModel::onFlightNumberChange,
        onConfirmationNumberChange = viewModel::onConfirmationNumberChange,
        onSetDefaultLeg = viewModel::onSetDefaultLeg,
        onDepartureDateTimeChange = viewModel::onDepartureDateTimeChange,
        onArrivalDateTimeChange = viewModel::onArrivalDateTimeChange,
        onNavigateToDocument = onNavigateToDocument,
        onClearLegSourceDocument = viewModel::onClearLegSourceDocument,
        modifier = modifier,
    )
}

/**
 * Stateless presentation of the Transport Detail screen.
 *
 * Accepts a [TransportDetailUiState] snapshot so it can be used in `@Preview` without a real
 * ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransportDetailContent(
    uiState: TransportDetailUiState,
    onNavigateUp: () -> Unit,
    onAddLeg: () -> Unit,
    onRemoveLeg: (Int) -> Unit,
    onMoveLegUp: (Int) -> Unit,
    onMoveLegDown: (Int) -> Unit,
    onTypeSelected: (Int, String?) -> Unit,
    onStopNameChange: (Int, String) -> Unit,
    onCompanyChange: (Int, String) -> Unit,
    onFlightNumberChange: (Int, String) -> Unit,
    onConfirmationNumberChange: (Int, String) -> Unit,
    onSetDefaultLeg: (Int) -> Unit = {},
    onDepartureDateTimeChange: (Int, ZonedDateTime?) -> Unit = { _, _ -> },
    onArrivalDateTimeChange: (Int, ZonedDateTime?) -> Unit = { _, _ -> },
    onNavigateToDocument: (Int) -> Unit = {},
    onClearLegSourceDocument: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(TransportDetailTab.DETAILS) }

    val title = when (uiState) {
        is TransportDetailUiState.Success -> uiState.destinationName
        else -> ""
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
                            contentDescription = stringResource(R.string.transport_detail_navigate_up),
                        )
                    }
                },
            )
        },
        bottomBar = {
            TransportDetailBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        },
    ) { innerPadding ->
        when (uiState) {
            is TransportDetailUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            is TransportDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(stringResource(R.string.transport_detail_not_found))
                }
            }

            is TransportDetailUiState.Success -> {
                when (selectedTab) {
                    TransportDetailTab.DETAILS -> TransportDetailsTabContent(
                        uiState = uiState,
                        innerPadding = innerPadding,
                    )
                    TransportDetailTab.LEGS -> TransportLegsTabContent(
                        uiState = uiState,
                        onAddLeg = onAddLeg,
                        onRemoveLeg = onRemoveLeg,
                        onMoveLegUp = onMoveLegUp,
                        onMoveLegDown = onMoveLegDown,
                        onTypeSelected = onTypeSelected,
                        onStopNameChange = onStopNameChange,
                        onCompanyChange = onCompanyChange,
                        onFlightNumberChange = onFlightNumberChange,
                        onConfirmationNumberChange = onConfirmationNumberChange,
                        onSetDefaultLeg = onSetDefaultLeg,
                        onDepartureDateTimeChange = onDepartureDateTimeChange,
                        onArrivalDateTimeChange = onArrivalDateTimeChange,
                        onNavigateToDocument = onNavigateToDocument,
                        onClearLegSourceDocument = onClearLegSourceDocument,
                        innerPadding = innerPadding,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransportDetailBottomBar(
    selectedTab: TransportDetailTab,
    onTabSelected: (TransportDetailTab) -> Unit,
) {
    NavigationBar {
        TransportDetailTab.entries.forEach { tab ->
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
 * Content for the Details tab: shows a summary of the destination transport including
 * the origin, each leg's type and stop, and the final destination.
 */
@Composable
private fun TransportDetailsTabContent(
    uiState: TransportDetailUiState.Success,
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Origin / destination header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.transport_detail_origin_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uiState.originName,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.transport_detail_destination_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uiState.nextDestinationName ?: stringResource(R.string.transport_detail_no_destination),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        if (uiState.legs.isNotEmpty()) {
            HorizontalDivider()

            // Journey summary: list each leg with its type and stop
            TransportJourneySummary(
                originName = uiState.originName,
                nextDestinationName = uiState.nextDestinationName,
                legs = uiState.legs,
            )
        } else {
            Text(
                text = stringResource(R.string.transport_none),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Renders the full journey chain: origin → leg1 type → leg1 stop → … → final destination.
 *
 * Each leg is represented by its transport type icon and label.  For non-last legs the stop name
 * (where that leg ends) is shown after the leg arrow when [TransportLegEditState.stopName] is set.
 * The last leg never shows an intermediate stop; it ends at [nextDestinationName], which is always
 * shown at the bottom of the chain when available.  This mirrors the Legs editing tab, which also
 * only displays intermediate stops between legs (never below the final leg).
 */
@Composable
private fun TransportJourneySummary(
    originName: String,
    nextDestinationName: String?,
    legs: List<TransportLegEditState>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Origin stop
        JourneyStop(name = originName, isOrigin = true)

        legs.forEachIndexed { index, leg ->
            val type = leg.typeName?.let { runCatching { TransportType.valueOf(it) }.getOrNull() }
            val legDuration = leg.departureDateTime.durationUntil(leg.arrivalDateTime)

            // Leg arrow with type label + optional duration
            Row(
                modifier = Modifier.padding(start = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (type != null) {
                    Icon(
                        imageVector = type.detailIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(type.detailLabelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (legDuration != null) {
                        Text(
                            text = "· ${legDuration.formatted()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.transport_detail_leg_number, index + 1),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Intermediate stop and layover are only shown for non-last legs, consistent with
            // the Legs editing tab which never shows a stop entry below the final leg.
            if (index < legs.lastIndex) {
                if (leg.stopName.isNotBlank()) {
                    JourneyStop(name = leg.stopName, isOrigin = false)
                }
                // Show layover duration below the stop: time between this leg's arrival and the
                // next leg's departure, accounting for timezone differences.
                val nextLeg = legs.getOrNull(index + 1)
                val layoverDuration = leg.arrivalDateTime.durationUntil(nextLeg?.departureDateTime)
                if (layoverDuration != null) {
                    Text(
                        text = stringResource(R.string.transport_detail_layover_duration, layoverDuration.formatted()),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 28.dp),
                    )
                }
            }
        }

        // Always show the overall final destination at the bottom of the chain.
        if (nextDestinationName != null) {
            JourneyStop(name = nextDestinationName, isOrigin = false)
        }
    }
}

@Composable
private fun JourneyStop(name: String, isOrigin: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isOrigin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/**
 * Content for the Legs tab: a timeline view showing origin → legs with editable intermediate
 * stops between them → final destination.  Move-up / move-down buttons allow reordering legs.
 */
@Composable
private fun TransportLegsTabContent(
    uiState: TransportDetailUiState.Success,
    onAddLeg: () -> Unit,
    onRemoveLeg: (Int) -> Unit,
    onMoveLegUp: (Int) -> Unit,
    onMoveLegDown: (Int) -> Unit,
    onTypeSelected: (Int, String?) -> Unit,
    onStopNameChange: (Int, String) -> Unit,
    onCompanyChange: (Int, String) -> Unit,
    onFlightNumberChange: (Int, String) -> Unit,
    onConfirmationNumberChange: (Int, String) -> Unit,
    onSetDefaultLeg: (Int) -> Unit,
    onDepartureDateTimeChange: (Int, ZonedDateTime?) -> Unit,
    onArrivalDateTimeChange: (Int, ZonedDateTime?) -> Unit,
    onNavigateToDocument: (Int) -> Unit = {},
    onClearLegSourceDocument: (Int) -> Unit = {},
    innerPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Origin location marker (not editable here)
        LegTimelineStop(name = uiState.originName, isOrigin = true)

        if (uiState.legs.isEmpty()) {
            Text(
                text = stringResource(R.string.transport_detail_no_legs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }

        uiState.legs.forEachIndexed { index, leg ->
            key(leg.clientKey) {
                val isLastLeg = index == uiState.legs.lastIndex
                // Compute date bounds from the parent destination and next destination so the
                // picker constrains leg dates within the overall transport window.
                val minDateMillis = uiState.destinationDepartureDateTime.toDateEpochMillis()
                val maxDateMillis = uiState.nextDestinationArrivalDateTime.toDateEpochMillis()
                // Infer the default timezone for each date picker from the surrounding items so
                // the user does not have to manually pick a zone when setting a fresh date.
                // For departure: prefer the previous leg's arrival zone, then fall back to the
                // origin destination's departure zone, then the device default.
                val defaultDepartureZoneId = (
                    if (index == 0) null else uiState.legs[index - 1].arrivalDateTime?.zone
                ) ?: uiState.destinationDepartureDateTime?.zone
                    ?: ZoneId.systemDefault()
                // For arrival: prefer the next leg's departure zone, then fall back to the
                // next destination's arrival zone, then the device default.
                val defaultArrivalZoneId = (
                    if (isLastLeg) null else uiState.legs[index + 1].departureDateTime?.zone
                ) ?: uiState.nextDestinationArrivalDateTime?.zone
                    ?: ZoneId.systemDefault()
                // Leg card: type selector + booking details + move controls.
                // Intermediate legs are deleted via their corresponding IntermediateLegStop
                // delete button below.
                TransportLegSection(
                    index = index,
                    totalLegs = uiState.legs.size,
                    leg = leg,
                    onMoveUp = { onMoveLegUp(index) },
                    onMoveDown = { onMoveLegDown(index) },
                    onTypeSelected = { typeName -> onTypeSelected(index, typeName) },
                    onCompanyChange = { value -> onCompanyChange(index, value) },
                    onFlightNumberChange = { value -> onFlightNumberChange(index, value) },
                    onConfirmationNumberChange = { value -> onConfirmationNumberChange(index, value) },
                    onSetDefault = { onSetDefaultLeg(index) },
                    onDepartureDateTimeChange = { dt -> onDepartureDateTimeChange(index, dt) },
                    onArrivalDateTimeChange = { dt -> onArrivalDateTimeChange(index, dt) },
                    onNavigateToDocument = onNavigateToDocument,
                    onClearSourceDocument = { onClearLegSourceDocument(index) },
                    minDateMillis = minDateMillis,
                    maxDateMillis = maxDateMillis,
                    defaultDepartureZoneId = defaultDepartureZoneId,
                    defaultArrivalZoneId = defaultArrivalZoneId,
                )

                // Editable intermediate stop shown only between legs.
                // The last leg ends at the final destination, which is already displayed below.
                // The delete button on the stop deletes the leg that ends there (this leg, at [index]),
                // which removes both the leg and its destination stop in one action.
                if (!isLastLeg) {
                    IntermediateLegStop(
                        stopName = leg.stopName,
                        onStopNameChange = { value -> onStopNameChange(index, value) },
                        onRemove = { onRemoveLeg(index) },
                    )
                    // Show layover duration: time between this leg's arrival and the next leg's departure.
                    val nextLeg = uiState.legs.getOrNull(index + 1)
                    val layoverDuration = leg.arrivalDateTime.durationUntil(nextLeg?.departureDateTime)
                    if (layoverDuration != null) {
                        Text(
                            text = stringResource(R.string.transport_detail_layover_duration, layoverDuration.formatted()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 28.dp, top = 2.dp),
                        )
                    }
                }
            }
        }

        // Final destination marker (not editable here)
        LegTimelineStop(
            name = uiState.nextDestinationName ?: stringResource(R.string.transport_detail_no_destination),
            isOrigin = false,
        )

        Spacer(modifier = Modifier.height(4.dp))

        TextButton(
            onClick = onAddLeg,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text(stringResource(R.string.transport_detail_add_leg))
        }
    }
}

/**
 * A read-only location marker shown at the top (origin) and bottom (final destination) of the
 * legs timeline.
 */
@Composable
private fun LegTimelineStop(
    name: String,
    isOrigin: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (isOrigin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isOrigin) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
        )
    }
}

/**
 * An editable intermediate stop shown between two leg cards. The stop name is displayed as
 * a tappable label; tapping it switches to an [OutlinedTextField] so the user can edit it
 * inline. Explicit Save (✓) and Cancel (✗) buttons commit or discard the change.
 *
 * @param onRemove Called when the user taps the delete button.  Deletes the leg that ends at
 *   this stop (i.e. the leg immediately before this stop in the timeline), which removes both
 *   the leg and this destination stop in one action.
 */
@Composable
private fun IntermediateLegStop(
    stopName: String,
    onStopNameChange: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        EditableLabel(
            value = stopName,
            onValueChange = onStopNameChange,
            label = stringResource(R.string.transport_detail_stop_name_label),
            modifier = Modifier.weight(1f),
        )
        val removeDescription = if (stopName.isNotBlank()) {
            stringResource(R.string.transport_detail_remove_leg_named, stopName)
        } else {
            stringResource(R.string.transport_detail_remove_leg)
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = removeDescription,
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun TransportLegSection(
    index: Int,
    totalLegs: Int,
    leg: TransportLegEditState,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onTypeSelected: (String?) -> Unit,
    onCompanyChange: (String) -> Unit,
    onFlightNumberChange: (String) -> Unit,
    onConfirmationNumberChange: (String) -> Unit,
    onSetDefault: () -> Unit,
    onDepartureDateTimeChange: (ZonedDateTime?) -> Unit,
    onArrivalDateTimeChange: (ZonedDateTime?) -> Unit,
    onNavigateToDocument: (Int) -> Unit = {},
    onClearSourceDocument: () -> Unit = {},
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
    defaultDepartureZoneId: ZoneId = ZoneId.systemDefault(),
    defaultArrivalZoneId: ZoneId = ZoneId.systemDefault(),
    modifier: Modifier = Modifier,
) {
    val selectedType = leg.typeName?.let { name ->
        runCatching { TransportType.valueOf(name) }.getOrNull()
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Leg header: leg number + set-default toggle (multi-leg only) + move up/down controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (totalLegs > 1) {
                        stringResource(R.string.transport_detail_leg_number, index + 1)
                    } else {
                        stringResource(R.string.transport_detail_type_label)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (totalLegs > 1) {
                    IconButton(onClick = onSetDefault, enabled = !leg.isDefault) {
                        Icon(
                            imageVector = if (leg.isDefault) {
                                Icons.Default.Star
                            } else {
                                Icons.Default.StarBorder
                            },
                            contentDescription = if (leg.isDefault) {
                                stringResource(R.string.transport_detail_default_leg)
                            } else {
                                stringResource(R.string.transport_detail_set_default_leg)
                            },
                            tint = if (leg.isDefault) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
                IconButton(onClick = onMoveUp, enabled = index > 0) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.transport_detail_move_leg_up),
                        tint = if (index > 0) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    )
                }
                IconButton(onClick = onMoveDown, enabled = index < totalLegs - 1) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.transport_detail_move_leg_down),
                        tint = if (index < totalLegs - 1) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        },
                    )
                }
            }

            // Transport type grid (rows of 4 icons)
            TransportType.entries.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    rowItems.forEach { type ->
                        TransportDetailOption(
                            icon = type.detailIcon,
                            label = stringResource(type.detailLabelRes),
                            isSelected = type == selectedType,
                            onClick = { onTypeSelected(type.name) },
                        )
                    }
                }
            }

            // "None" option to clear the transport type for this leg
            Row(modifier = Modifier.fillMaxWidth()) {
                TransportDetailOption(
                    icon = Icons.Default.Close,
                    label = stringResource(R.string.transport_none),
                    isSelected = selectedType == null,
                    onClick = { onTypeSelected(null) },
                )
            }

            if (selectedType != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Departure date/time – shown on all legs; for the first leg this is also the
                // transport departure and is kept in sync with the destination's departure.
                LegDateTimeRow(
                    label = stringResource(R.string.transport_detail_departure_label),
                    dateTime = leg.departureDateTime,
                    onDateTimeChange = onDepartureDateTimeChange,
                    minDateMillis = minDateMillis,
                    maxDateMillis = maxDateMillis,
                    defaultZoneId = defaultDepartureZoneId,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Arrival date/time – shown on all legs; for the last leg this is also the
                // transport arrival and is kept in sync with the next destination's arrival.
                LegDateTimeRow(
                    label = stringResource(R.string.transport_detail_arrival_label),
                    dateTime = leg.arrivalDateTime,
                    onDateTimeChange = onArrivalDateTimeChange,
                    minDateMillis = minDateMillis,
                    maxDateMillis = maxDateMillis,
                    defaultZoneId = defaultArrivalZoneId,
                    modifier = Modifier.fillMaxWidth(),
                )

                // Show leg duration when both departure and arrival are known.
                val legDuration = leg.departureDateTime.durationUntil(leg.arrivalDateTime)
                if (legDuration != null) {
                    Text(
                        text = stringResource(R.string.transport_detail_leg_duration, legDuration.formatted()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                EditableLabel(
                    value = leg.company,
                    onValueChange = onCompanyChange,
                    label = stringResource(R.string.transport_company_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                EditableLabel(
                    value = leg.flightNumber,
                    onValueChange = onFlightNumberChange,
                    label = stringResource(R.string.transport_flight_number_label),
                    modifier = Modifier.fillMaxWidth(),
                )
                EditableLabel(
                    value = leg.confirmationNumber,
                    onValueChange = onConfirmationNumberChange,
                    label = stringResource(R.string.transport_confirmation_label),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (leg.sourceDocumentId != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SourceDocumentChip(
                        documentName = leg.sourceDocumentName,
                        onDocumentClick = { onNavigateToDocument(leg.sourceDocumentId) },
                        onRemove = onClearSourceDocument,
                    )
                }
            }
        }
    }
}

/**
 * A labelled row showing date, time, and timezone buttons for a single [ZonedDateTime] value on a leg.
 *
 * - Tapping the date button opens a [DatePickerDialog].
 * - Tapping the time button opens a [TimePicker] dialog (requires a date to be set first).
 * - Tapping the timezone button opens a timezone picker dialog (requires a date to be set first).
 * - Selecting a new date preserves the existing time (or defaults to midnight) and zone.
 * - Changing the timezone keeps the same local date and time, only the zone offset changes.
 *
 * @param minDateMillis Optional lower bound (inclusive) for selectable dates, in epoch-day
 *   milliseconds.  Dates before this value are disabled in the picker.
 * @param maxDateMillis Optional upper bound (inclusive) for selectable dates, in epoch-day
 *   milliseconds.  Dates after this value are disabled in the picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LegDateTimeRow(
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
        // Normalize bounds so an inverted range (e.g. from out-of-order existing data) doesn't
        // disable all dates and lock the user out of correcting them.
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
        // Open the picker at the current selection, falling back to the min bound so the user
        // sees a relevant month rather than today when the trip dates are far away.
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

/**
 * A labelled text value that switches to an [OutlinedTextField] when tapped.
 *
 * When not in edit mode the field looks like a stacked label + value pair, keeping the legs
 * tab uncluttered. Tapping the row activates an [OutlinedTextField] with the field label as
 * a floating label. Explicit Save (✓) and Cancel (✗) icon buttons commit or discard the
 * change; pressing the Done IME action also commits.
 *
 * @param value The current text value.
 * @param onValueChange Called with the updated text when the user confirms the edit.
 * @param label The short descriptive label shown above the value in read-only mode and as the
 *   floating label in edit mode.
 */
@Composable
internal fun EditableLabel(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
) {
    var isEditing by rememberSaveable { mutableStateOf(false) }
    var draft by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val editActionLabel = stringResource(R.string.editable_label_edit_action, label)
    val notSetPlaceholder = stringResource(R.string.editable_label_not_set)
    if (isEditing) {
        fun commitEdit() {
            if (draft != value) {
                onValueChange(draft)
            }
            isEditing = false
        }
        fun cancelEdit() {
            isEditing = false
        }
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { commitEdit() }),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
            )
            IconButton(onClick = { commitEdit() }) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.editable_label_save, label),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = { cancelEdit() }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.editable_label_cancel, label),
                )
            }
        }
        LaunchedEffect(isEditing) {
            if (isEditing) {
                focusRequester.requestFocus()
            }
        }
    } else {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.Button, onClickLabel = editActionLabel) {
                    draft = value
                    isEditing = true
                }
                .padding(vertical = 4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value.ifBlank { notSetPlaceholder },
                style = MaterialTheme.typography.bodyMedium,
                color = if (value.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun TransportDetailOption(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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

/** Maps a [TransportType] to its icon for use on the Transport Detail screen. */
private val TransportType.detailIcon: ImageVector
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

/** Maps a [TransportType] to its string resource for use on the Transport Detail screen. */
private val TransportType.detailLabelRes: Int
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

// ── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun TransportDetailMultiLegPreview() {
    WanderVaultTheme {
        TransportDetailContent(
            uiState = TransportDetailUiState.Success(
                destinationName = "Paris",
                originName = "Paris",
                nextDestinationName = "Rome",
                legs = listOf(
                    TransportLegEditState(
                        id = 1,
                        typeName = TransportType.DRIVING.name,
                        stopName = "CDG Airport",
                        company = "Uber",
                    ),
                    TransportLegEditState(
                        id = 2,
                        typeName = TransportType.FLIGHT.name,
                        stopName = "FCO Airport",
                        company = "Air France",
                        flightNumber = "AF1234",
                        confirmationNumber = "XYZ123",
                    ),
                    TransportLegEditState(
                        id = 3,
                        typeName = TransportType.TRAIN.name,
                        company = "Trenitalia",
                    ),
                ),
            ),
            onNavigateUp = {},
            onAddLeg = {},
            onRemoveLeg = {},
            onMoveLegUp = {},
            onMoveLegDown = {},
            onTypeSelected = { _, _ -> },
            onStopNameChange = { _, _ -> },
            onCompanyChange = { _, _ -> },
            onFlightNumberChange = { _, _ -> },
            onConfirmationNumberChange = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransportDetailSingleLegPreview() {
    WanderVaultTheme {
        TransportDetailContent(
            uiState = TransportDetailUiState.Success(
                destinationName = "London",
                originName = "London",
                nextDestinationName = "Paris",
                legs = listOf(
                    TransportLegEditState(
                        typeName = TransportType.TRAIN.name,
                        stopName = "Paris Gare du Nord",
                        company = "Eurostar",
                        flightNumber = "ES9001",
                        confirmationNumber = "TRAIN-456",
                    ),
                ),
            ),
            onNavigateUp = {},
            onAddLeg = {},
            onRemoveLeg = {},
            onMoveLegUp = {},
            onMoveLegDown = {},
            onTypeSelected = { _, _ -> },
            onStopNameChange = { _, _ -> },
            onCompanyChange = { _, _ -> },
            onFlightNumberChange = { _, _ -> },
            onConfirmationNumberChange = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransportDetailNoLegsPreview() {
    WanderVaultTheme {
        TransportDetailContent(
            uiState = TransportDetailUiState.Success(
                destinationName = "Rome",
                originName = "Rome",
                nextDestinationName = "Florence",
                legs = emptyList(),
            ),
            onNavigateUp = {},
            onAddLeg = {},
            onRemoveLeg = {},
            onMoveLegUp = {},
            onMoveLegDown = {},
            onTypeSelected = { _, _ -> },
            onStopNameChange = { _, _ -> },
            onCompanyChange = { _, _ -> },
            onFlightNumberChange = { _, _ -> },
            onConfirmationNumberChange = { _, _ -> },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TransportDetailLoadingPreview() {
    WanderVaultTheme {
        TransportDetailContent(
            uiState = TransportDetailUiState.Loading,
            onNavigateUp = {},
            onAddLeg = {},
            onRemoveLeg = {},
            onMoveLegUp = {},
            onMoveLegDown = {},
            onTypeSelected = { _, _ -> },
            onStopNameChange = { _, _ -> },
            onCompanyChange = { _, _ -> },
            onFlightNumberChange = { _, _ -> },
            onConfirmationNumberChange = { _, _ -> },
        )
    }
}
