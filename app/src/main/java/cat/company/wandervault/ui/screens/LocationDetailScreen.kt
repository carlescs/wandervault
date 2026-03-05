package cat.company.wandervault.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Location Detail screen entry point.
 *
 * Displays the summary information for a single destination stop.
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
    modifier: Modifier = Modifier,
) {
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
    ) { innerPadding ->
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
private fun LocationDetailPreview() {
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
            uiState = LocationDetailUiState(destination = destination),
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
            uiState = LocationDetailUiState(destination = destination),
            onNavigateUp = {},
        )
    }
}
