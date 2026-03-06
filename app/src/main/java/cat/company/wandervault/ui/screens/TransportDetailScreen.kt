package cat.company.wandervault.ui.screens

import androidx.annotation.StringRes
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
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
import androidx.compose.material.icons.filled.Train
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel

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
    modifier: Modifier = Modifier,
    viewModel: TransportDetailViewModel = koinViewModel(
        key = "TransportDetailViewModel:$destinationId",
    ),
) {
    LaunchedEffect(destinationId) {
        viewModel.loadDestination(destinationId)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TransportDetailContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onAddLeg = viewModel::onAddLeg,
        onRemoveLeg = viewModel::onRemoveLeg,
        onMoveLegUp = viewModel::onMoveLegUp,
        onMoveLegDown = viewModel::onMoveLegDown,
        onTypeSelected = viewModel::onTypeSelected,
        onStopNameChange = viewModel::onStopNameChange,
        onCompanyChange = viewModel::onCompanyChange,
        onFlightNumberChange = viewModel::onFlightNumberChange,
        onConfirmationNumberChange = viewModel::onConfirmationNumberChange,
        onSave = {
            viewModel.onSave()
            onNavigateUp()
        },
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
    onSave: () -> Unit,
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
                actions = {
                    TextButton(
                        onClick = onSave,
                        enabled = uiState is TransportDetailUiState.Success,
                    ) {
                        Text(stringResource(R.string.dialog_save))
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
 * Each leg is represented by its transport type icon and label.  The stop name (where that leg
 * ends) is shown after each leg arrow when [TransportLegEditState.stopName] is set.  The final
 * destination ([nextDestinationName]) is always shown at the bottom of the chain when available.
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

            // Leg arrow with type label
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
                } else {
                    Text(
                        text = stringResource(R.string.transport_detail_leg_number, index + 1),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Intermediate stop where this leg ends (only shown when stopName is set and
            // it is not the last leg, or when it differs from nextDestinationName).
            if (leg.stopName.isNotBlank() && (index < legs.lastIndex || leg.stopName != nextDestinationName)) {
                JourneyStop(name = leg.stopName, isOrigin = false)
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
            // Leg card: type selector + booking details + move/delete controls
            TransportLegSection(
                index = index,
                totalLegs = uiState.legs.size,
                leg = leg,
                onRemove = { onRemoveLeg(index) },
                onMoveUp = { onMoveLegUp(index) },
                onMoveDown = { onMoveLegDown(index) },
                onTypeSelected = { typeName -> onTypeSelected(index, typeName) },
                onCompanyChange = { value -> onCompanyChange(index, value) },
                onFlightNumberChange = { value -> onFlightNumberChange(index, value) },
                onConfirmationNumberChange = { value -> onConfirmationNumberChange(index, value) },
            )

            // Editable intermediate stop shown after every leg.
            // For the last leg this is the stop before the final destination (may be blank).
            IntermediateLegStop(
                stopName = leg.stopName,
                onStopNameChange = { value -> onStopNameChange(index, value) },
            )
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
 * An editable intermediate stop shown between two leg cards (or between the last leg card and
 * the final destination).  When [stopName] is blank the field is shown as a placeholder.
 */
@Composable
private fun IntermediateLegStop(
    stopName: String,
    onStopNameChange: (String) -> Unit,
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
        OutlinedTextField(
            value = stopName,
            onValueChange = onStopNameChange,
            label = { Text(stringResource(R.string.transport_detail_stop_name_label)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TransportLegSection(
    index: Int,
    totalLegs: Int,
    leg: TransportLegEditState,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onTypeSelected: (String?) -> Unit,
    onCompanyChange: (String) -> Unit,
    onFlightNumberChange: (String) -> Unit,
    onConfirmationNumberChange: (String) -> Unit,
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
            // Leg header: leg number + move up/down + delete
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
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.transport_detail_remove_leg),
                        tint = MaterialTheme.colorScheme.error,
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

                OutlinedTextField(
                    value = leg.company,
                    onValueChange = onCompanyChange,
                    label = { Text(stringResource(R.string.transport_company_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = leg.flightNumber,
                    onValueChange = onFlightNumberChange,
                    label = { Text(stringResource(R.string.transport_flight_number_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = leg.confirmationNumber,
                    onValueChange = onConfirmationNumberChange,
                    label = { Text(stringResource(R.string.transport_confirmation_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
            onSave = {},
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
            onSave = {},
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
            onSave = {},
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
            onSave = {},
        )
    }
}
