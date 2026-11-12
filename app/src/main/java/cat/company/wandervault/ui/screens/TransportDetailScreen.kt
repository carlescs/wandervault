package cat.company.wandervault.ui.screens

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.ZonedDateTime

/** Width of the rail column on the legs timeline. */
private const val LEG_RAIL_WIDTH_DP = 32

/** Height of the origin / final-destination rows; the rail starts and ends at their centre. */
private const val LEG_ENDPOINT_ROW_HEIGHT_DP = 56

/** Stroke width of the legs timeline rail. */
private const val LEG_RAIL_STROKE_DP = 3

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
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val imagePicker = rememberDocumentImagePicker(onImageSelected = viewModel::onScanDocument)
    // Remove the temporary camera capture as soon as it is no longer being scanned.
    LaunchedEffect(scanState) {
        if (scanState != DocumentScanUiState.Scanning) imagePicker.deleteCapture()
    }
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
        scanState = scanState,
        onTakePhoto = imagePicker::takePhoto,
        onPickImage = imagePicker::pickImage,
        onApplyScannedTransport = viewModel::onApplyScannedTransport,
        onDismissScan = viewModel::onDismissScan,
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
    scanState: DocumentScanUiState = DocumentScanUiState.Idle,
    onTakePhoto: () -> Unit = {},
    onPickImage: () -> Unit = {},
    onApplyScannedTransport: (Int?, String, String, String) -> Unit = { _, _, _, _ -> },
    onDismissScan: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableStateOf(TransportDetailTab.DETAILS) }

    DocumentScanStatusDialogs(state = scanState, onDismiss = onDismissScan)
    if (scanState is DocumentScanUiState.Review && uiState is TransportDetailUiState.Success) {
        ScannedTransportReviewDialog(
            info = scanState.info,
            legs = uiState.legs,
            suggestedLegIndex = scanState.suggestedLegIndex,
            onConfirm = { legIndex, flightNumber, confirmation, company ->
                onApplyScannedTransport(legIndex, flightNumber, confirmation, company)
                selectedTab = TransportDetailTab.LEGS
            },
            onDismiss = onDismissScan,
        )
    }

    val title = when (uiState) {
        is TransportDetailUiState.Success -> uiState.destinationName
        else -> ""
    }
    Scaffold(
        modifier = modifier,
        topBar = {
            Column {
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
                        if (uiState is TransportDetailUiState.Success) {
                            ScanDocumentAction(
                                onTakePhoto = onTakePhoto,
                                onPickImage = onPickImage,
                                enabled = scanState == DocumentScanUiState.Idle,
                            )
                        }
                    },
                )
                DetailTabRow(
                    tabs = TransportDetailTab.entries,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    labelRes = { it.labelRes },
                    icon = { it.icon },
                )
            }
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
                DetailMessageState(
                    message = stringResource(R.string.transport_detail_not_found),
                    modifier = Modifier.padding(innerPadding),
                )
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
                    style = MaterialTheme.typography.titleLarge,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .align(Alignment.CenterVertically),
            )
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.transport_detail_destination_label),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = uiState.nextDestinationName ?: stringResource(R.string.transport_detail_no_destination),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.End,
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
    val railColor = MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    // One continuous rail from the origin node to the final destination node;
                    // the leg and stop nodes are drawn on top of it.
                    val x = (LEG_RAIL_WIDTH_DP / 2).dp.toPx()
                    val endpointCenter = (LEG_ENDPOINT_ROW_HEIGHT_DP / 2).dp.toPx()
                    drawLine(
                        color = railColor,
                        start = Offset(x, endpointCenter),
                        end = Offset(x, size.height - endpointCenter),
                        strokeWidth = LEG_RAIL_STROKE_DP.dp.toPx(),
                    )
                },
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Origin location marker (not editable here)
            LegTimelineStop(name = uiState.originName, isOrigin = true)

            if (uiState.legs.isEmpty()) {
                Text(
                    text = stringResource(R.string.transport_detail_no_legs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = LEG_RAIL_WIDTH_DP.dp + 12.dp, top = 8.dp, bottom = 8.dp),
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
                    val legType = leg.typeName?.let { name ->
                        runCatching { TransportType.valueOf(name) }.getOrNull()
                    }
                    LegTimelineRow(
                        nodeTopPadding = 16.dp,
                        node = { LegTypeNode(type = legType) },
                    ) {
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
                    }

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
                                modifier = Modifier.padding(start = LEG_RAIL_WIDTH_DP.dp + 12.dp),
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        FilledTonalButton(
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
 * A row on the legs timeline: a [node] drawn over the rail on the left, and [content] on the
 * right. [nodeTopPadding] aligns the node with the content's header.
 */
@Composable
private fun LegTimelineRow(
    nodeTopPadding: Dp,
    node: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .width(LEG_RAIL_WIDTH_DP.dp)
                .padding(top = nodeTopPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            node()
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            content()
        }
    }
}

/** Circular node showing the transport type icon of a leg, drawn over the timeline rail. */
@Composable
private fun LegTypeNode(type: TransportType?, modifier: Modifier = Modifier) {
    IconBadge(
        icon = type?.detailIcon ?: Icons.Default.MoreHoriz,
        modifier = modifier,
        containerColor = if (type != null) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        contentColor = if (type != null) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        size = LEG_RAIL_WIDTH_DP.dp,
    )
}

/**
 * A read-only location marker shown at the top (origin) and bottom (final destination) of the
 * legs timeline: a filled pin node on the rail, a "From" / "To" caption and the place name.
 */
@Composable
private fun LegTimelineStop(
    name: String,
    isOrigin: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(LEG_ENDPOINT_ROW_HEIGHT_DP.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(LEG_RAIL_WIDTH_DP.dp),
            contentAlignment = Alignment.Center,
        ) {
            IconBadge(
                icon = Icons.Default.LocationOn,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                size = 28.dp,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                text = stringResource(
                    if (isOrigin) R.string.transport_detail_origin_label else R.string.transport_detail_destination_label,
                ),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * An editable intermediate stop shown between two leg cards, with a ring node on the timeline
 * rail. The stop name is displayed as a tappable label; tapping it switches to an
 * [OutlinedTextField] so the user can edit it inline. Explicit Save (✓) and Cancel (✗) buttons
 * commit or discard the change.
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
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.width(LEG_RAIL_WIDTH_DP.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
        EditableLabel(
            value = stopName,
            onValueChange = onStopNameChange,
            label = stringResource(R.string.transport_detail_stop_name_label),
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        )
        val removeDescription = if (stopName.isNotBlank()) {
            stringResource(R.string.transport_detail_remove_leg_named, stopName)
        } else {
            stringResource(R.string.transport_detail_remove_leg)
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
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
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
                DateTimeField(
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
                DateTimeField(
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

                leg.sourceDocumentId?.let { sourceDocumentId ->
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    SourceDocumentChip(
                        documentName = leg.sourceDocumentName,
                        onDocumentClick = { onNavigateToDocument(sourceDocumentId) },
                        onRemove = onClearSourceDocument,
                    )
                }
            }
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .defaultMinSize(minHeight = 48.dp)
                .clickable(role = Role.Button, onClickLabel = editActionLabel) {
                    draft = value
                    isEditing = true
                }
                .padding(vertical = 4.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
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
            // Visual hint that the field is tappable; the click label already announces it.
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp),
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
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val shape = MaterialTheme.shapes.medium
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 64.dp)
            .clip(shape)
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                shape = shape,
            )
            .selectable(selected = isSelected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 6.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.height(4.dp))
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
internal val TransportType.detailLabelRes: Int
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
