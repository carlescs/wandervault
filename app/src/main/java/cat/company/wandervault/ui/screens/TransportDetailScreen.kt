package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsBoat
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Flight
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
        onTypeSelected = viewModel::onTypeSelected,
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
    onTypeSelected: (Int, String?) -> Unit,
    onCompanyChange: (Int, String) -> Unit,
    onFlightNumberChange: (Int, String) -> Unit,
    onConfirmationNumberChange: (Int, String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    // Outer card: the "main transport" for this destination.
                    // All legs are children rendered inside it.
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        ),
                    ) {
                        Column {
                            if (uiState.legs.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.transport_detail_no_legs),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                            }

                            uiState.legs.forEachIndexed { index, leg ->
                                if (index > 0) {
                                    HorizontalDivider()
                                }
                                TransportLegSection(
                                    index = index,
                                    totalLegs = uiState.legs.size,
                                    leg = leg,
                                    onRemove = { onRemoveLeg(index) },
                                    onTypeSelected = { typeName -> onTypeSelected(index, typeName) },
                                    onCompanyChange = { value -> onCompanyChange(index, value) },
                                    onFlightNumberChange = { value -> onFlightNumberChange(index, value) },
                                    onConfirmationNumberChange = { value -> onConfirmationNumberChange(index, value) },
                                )
                            }

                            if (uiState.legs.isNotEmpty()) {
                                HorizontalDivider()
                            }

                            TextButton(
                                onClick = onAddLeg,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 4.dp),
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
                }
            }
        }
    }
}

@Composable
private fun TransportLegSection(
    index: Int,
    totalLegs: Int,
    leg: TransportLegEditState,
    onRemove: () -> Unit,
    onTypeSelected: (String?) -> Unit,
    onCompanyChange: (String) -> Unit,
    onFlightNumberChange: (String) -> Unit,
    onConfirmationNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedType = leg.typeName?.let { name ->
        runCatching { TransportType.valueOf(name) }.getOrNull()
    }

    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Leg header: leg number + delete button
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
            )
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
                legs = listOf(
                    TransportLegEditState(
                        id = 1,
                        typeName = TransportType.DRIVING.name,
                        company = "Uber",
                    ),
                    TransportLegEditState(
                        id = 2,
                        typeName = TransportType.FLIGHT.name,
                        company = "Air France",
                        flightNumber = "AF1234",
                        confirmationNumber = "XYZ123",
                    ),
                    TransportLegEditState(
                        id = 3,
                        typeName = TransportType.TRAIN.name,
                        company = "RATP",
                    ),
                ),
            ),
            onNavigateUp = {},
            onAddLeg = {},
            onRemoveLeg = {},
            onTypeSelected = { _, _ -> },
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
                legs = listOf(
                    TransportLegEditState(
                        typeName = TransportType.TRAIN.name,
                        company = "Eurostar",
                        flightNumber = "ES9001",
                        confirmationNumber = "TRAIN-456",
                    ),
                ),
            ),
            onNavigateUp = {},
            onAddLeg = {},
            onRemoveLeg = {},
            onTypeSelected = { _, _ -> },
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
                legs = emptyList(),
            ),
            onNavigateUp = {},
            onAddLeg = {},
            onRemoveLeg = {},
            onTypeSelected = { _, _ -> },
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
            onTypeSelected = { _, _ -> },
            onCompanyChange = { _, _ -> },
            onFlightNumberChange = { _, _ -> },
            onConfirmationNumberChange = { _, _ -> },
            onSave = {},
        )
    }
}
