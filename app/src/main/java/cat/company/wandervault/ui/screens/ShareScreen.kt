package cat.company.wandervault.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.ActivityInfo
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.Hotel
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.TransportLeg
import cat.company.wandervault.domain.model.TransportType
import cat.company.wandervault.domain.model.Trip
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Overlay dialog that handles sharing a document into WanderVault.
 *
 * The dialog walks the user through:
 * 1. Selecting which trip to attach the document to.
 * 2. (Automatic) Copying and analysing the document with ML Kit.
 * 3. (Optional) Resolving ambiguity when ML Kit finds structured info but cannot determine
 *    the exact itinerary element (flight leg or hotel) to update.
 *
 * The composable is shown when [shareIntent] is non-null and dismissed via [onDismiss].
 *
 * @param shareIntent The `ACTION_SEND` intent received from the system.
 * @param onDismiss Called when the flow is complete (success, error, or user cancellation).
 */
@Composable
fun ShareScreen(shareIntent: Intent, onDismiss: () -> Unit) {
    val sharedUri: String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        shareIntent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.toString() ?: ""
    } else {
        @Suppress("DEPRECATION")
        shareIntent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.toString() ?: ""
    }

    if (sharedUri.isBlank()) {
        // The intent has no stream URI (e.g. plain-text share). Show an error for the user to dismiss.
        ShareErrorDialog(onDismiss = onDismiss)
    } else {
        ShareScreenContent(shareIntent = shareIntent, sharedUri = sharedUri, onDismiss = onDismiss)
    }
}

@Composable
private fun ShareScreenContent(shareIntent: Intent, sharedUri: String, onDismiss: () -> Unit) {
    val context = LocalContext.current

    val mimeType: String = context.contentResolver.getType(Uri.parse(sharedUri))
        ?: (shareIntent.type ?: "*/*")
    val documentName: String = remember(sharedUri) {
        runCatching {
            context.contentResolver.query(
                Uri.parse(sharedUri),
                arrayOf(android.provider.OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
            }
        }.getOrNull() ?: ""
    }.ifBlank { stringResource(R.string.share_document_name_fallback) }

    // Key the ViewModel by the URI string (stable for a given document, unlike
    // System.identityHashCode which changes when the Intent object is recreated on configuration
    // changes). This means the same document reuses the same ViewModel instance across config
    // changes, while a different document gets its own instance. The hash of the URI is used to
    // avoid issues with special characters in the key.
    val viewModel: ShareViewModel = koinViewModel(
        key = "ShareViewModel:${sharedUri.hashCode()}",
        parameters = { parametersOf(sharedUri, mimeType, documentName) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto-dismiss when the flow completes.
    LaunchedEffect(uiState) {
        if (uiState is ShareUiState.Done) {
            onDismiss()
        }
    }

    when (val state = uiState) {
        ShareUiState.Loading -> {
            ShareLoadingDialog(onDismiss = onDismiss)
        }

        is ShareUiState.TripSelection -> {
            ShareTripSelectionDialog(
                documentName = state.documentName,
                trips = state.trips,
                onTripSelected = { trip -> viewModel.onTripSelected(trip.id) },
                onDismiss = onDismiss,
            )
        }

        ShareUiState.Processing -> {
            ShareProcessingDialog()
        }

        is ShareUiState.FlightLegSelection -> {
            ShareFlightLegSelectionDialog(
                flightInfo = state.flightInfo,
                candidates = state.candidates,
                onLegSelected = viewModel::onFlightLegSelected,
                onSkip = viewModel::onDisambiguationSkipped,
            )
        }

        is ShareUiState.FlightConfirm -> {
            ShareFlightConfirmDialog(
                flightInfo = state.flightInfo,
                matchedLeg = state.matchedLeg,
                onConfirm = viewModel::onFlightConfirmed,
                onDismiss = viewModel::onConfirmCancelled,
            )
        }

        is ShareUiState.HotelDestinationSelection -> {
            ShareHotelDestinationSelectionDialog(
                hotelInfo = state.hotelInfo,
                candidates = state.candidates,
                onDestinationSelected = viewModel::onHotelDestinationSelected,
                onSkip = viewModel::onDisambiguationSkipped,
            )
        }

        is ShareUiState.HotelConfirm -> {
            ShareHotelConfirmDialog(
                hotelInfo = state.hotelInfo,
                destination = state.destination,
                existingHotel = state.existingHotel,
                onConfirm = viewModel::onHotelConfirmed,
                onDismiss = viewModel::onConfirmCancelled,
            )
        }

        is ShareUiState.ActivityDestinationSelection -> {
            ShareActivityDestinationSelectionDialog(
                activityInfo = state.activityInfo,
                candidates = state.candidates,
                onDestinationSelected = viewModel::onActivityDestinationSelected,
                onSkip = viewModel::onDisambiguationSkipped,
            )
        }

        is ShareUiState.ActivityConfirm -> {
            ShareActivityConfirmDialog(
                activityInfo = state.activityInfo,
                destination = state.destination,
                onConfirm = viewModel::onActivityConfirmed,
                onDismiss = viewModel::onConfirmCancelled,
            )
        }

        ShareUiState.Done -> {
            // Handled by the LaunchedEffect above – no UI needed.
        }

        ShareUiState.Error -> {
            ShareErrorDialog(onDismiss = onDismiss)
        }
    }
}

// ---------------------------------------------------------------------------
// Stateless dialog composables (each handles one state)
// ---------------------------------------------------------------------------

@Composable
private fun ShareLoadingDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_title)) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun ShareTripSelectionDialog(
    documentName: String,
    trips: List<Trip>,
    onTripSelected: (Trip) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.share_select_trip_label, documentName),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                if (trips.isEmpty()) {
                    Text(
                        text = stringResource(R.string.share_no_trips),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(trips) { trip ->
                            TripSelectionItem(
                                trip = trip,
                                onClick = { onTripSelected(trip) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun TripSelectionItem(trip: Trip, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = trip.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ShareProcessingDialog() {
    AlertDialog(
        onDismissRequest = { /* non-cancellable while processing */ },
        title = { Text(stringResource(R.string.share_title)) },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text(
                    text = stringResource(R.string.share_processing),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun ShareFlightLegSelectionDialog(
    flightInfo: FlightInfo,
    candidates: List<TransportLeg>,
    onLegSelected: (TransportLeg) -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(R.string.share_flight_selection_title)) },
        text = {
            Column {
                FlightInfoSummary(flightInfo = flightInfo)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.share_flight_selection_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(candidates) { leg ->
                        FlightLegItem(
                            leg = leg,
                            onClick = { onLegSelected(leg) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.share_skip))
            }
        },
    )
}

@Composable
private fun FlightInfoSummary(flightInfo: FlightInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.AirplanemodeActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            val flightLabel = listOfNotNull(flightInfo.airline, flightInfo.flightNumber)
                .joinToString(" ")
                .ifBlank { stringResource(R.string.share_unknown_flight) }
            Text(text = flightLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (!flightInfo.bookingReference.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.share_booking_ref, flightInfo.bookingReference),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FlightLegItem(leg: TransportLeg, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.AirplanemodeActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        FlightLegDetails(leg = leg)
    }
}

/** Renders the label, booking reference and stop name for a [TransportLeg]. */
@Composable
private fun FlightLegDetails(leg: TransportLeg, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        val label = listOfNotNull(leg.company, leg.flightNumber)
            .joinToString(" ")
            .ifBlank { stringResource(R.string.share_unnamed_flight_leg) }
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        if (!leg.reservationConfirmationNumber.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.share_booking_ref, leg.reservationConfirmationNumber),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (!leg.stopName.isNullOrBlank()) {
            Text(
                text = stringResource(R.string.documents_analyze_to, leg.stopName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ShareHotelDestinationSelectionDialog(
    hotelInfo: HotelInfo,
    candidates: List<Destination>,
    onDestinationSelected: (Destination) -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(R.string.share_hotel_selection_title)) },
        text = {
            Column {
                HotelInfoSummary(hotelInfo = hotelInfo)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.share_hotel_selection_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(candidates) { destination ->
                        DestinationItem(
                            destination = destination,
                            onClick = { onDestinationSelected(destination) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.share_skip))
            }
        },
    )
}

@Composable
private fun HotelInfoSummary(hotelInfo: HotelInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Hotel,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            val hotelLabel = hotelInfo.name?.ifBlank { null }
                ?: stringResource(R.string.share_unknown_hotel)
            Text(text = hotelLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (!hotelInfo.bookingReference.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.share_booking_ref, hotelInfo.bookingReference),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ShareFlightConfirmDialog(
    flightInfo: FlightInfo,
    matchedLeg: TransportLeg,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.documents_analyze_confirm_flight_title)) },
        text = {
            Column {
                FlightInfoSummary(flightInfo = flightInfo)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.documents_analyze_confirm_flight_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.AirplanemodeActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    FlightLegDetails(leg = matchedLeg)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.documents_analyze_confirm_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun ShareHotelConfirmDialog(
    hotelInfo: HotelInfo,
    destination: Destination,
    existingHotel: Hotel?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.documents_analyze_confirm_hotel_title)) },
        text = {
            Column {
                HotelInfoSummary(hotelInfo = hotelInfo)
                Spacer(Modifier.height(4.dp))
                if (existingHotel != null) {
                    Text(
                        text = stringResource(
                            R.string.documents_analyze_confirm_hotel_message,
                            destination.name,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.documents_analyze_confirm_hotel_new_message,
                            destination.name,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = destination.name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val arrival = destination.arrivalDateTime?.toLocalDate()
                        val departure = destination.departureDateTime?.toLocalDate()
                        if (arrival != null || departure != null) {
                            val dateRange = when {
                                arrival != null && departure != null ->
                                    "${arrival.format(dateFormatter)} – ${departure.format(dateFormatter)}"
                                arrival != null -> arrival.format(dateFormatter)
                                else -> requireNotNull(departure).format(dateFormatter)
                            }
                            Text(
                                text = dateRange,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        existingHotel?.name?.ifBlank { null }?.let { hotelName ->
                            Text(
                                text = hotelName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.documents_analyze_confirm_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun DestinationItem(
    destination: Destination,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = destination.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val arrival = destination.arrivalDateTime?.toLocalDate()
            val departure = destination.departureDateTime?.toLocalDate()
            if (arrival != null || departure != null) {
                val dateRange = when {
                    arrival != null && departure != null ->
                        "${arrival.format(dateFormatter)} – ${departure.format(dateFormatter)}"
                    arrival != null -> arrival.format(dateFormatter)
                    else -> requireNotNull(departure).format(dateFormatter)
                }
                Text(
                    text = dateRange,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ShareActivityDestinationSelectionDialog(
    activityInfo: ActivityInfo,
    candidates: List<Destination>,
    onDestinationSelected: (Destination) -> Unit,
    onSkip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onSkip,
        title = { Text(stringResource(R.string.documents_analyze_activity_selection_title)) },
        text = {
            Column {
                ActivityInfoSummary(activityInfo = activityInfo)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.documents_analyze_activity_selection_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(candidates) { destination ->
                        DestinationItem(
                            destination = destination,
                            onClick = { onDestinationSelected(destination) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.share_skip))
            }
        },
    )
}

@Composable
private fun ShareActivityConfirmDialog(
    activityInfo: ActivityInfo,
    destination: Destination,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.documents_analyze_confirm_activity_title)) },
        text = {
            Column {
                ActivityInfoSummary(activityInfo = activityInfo)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.documents_analyze_confirm_activity_message,
                        destination.name,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = destination.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.documents_analyze_confirm_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        },
    )
}

@Composable
private fun ActivityInfoSummary(activityInfo: ActivityInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.EventNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Column {
            val activityLabel = activityInfo.title?.ifBlank { null }
                ?: stringResource(R.string.documents_analyze_activity_unknown)
            Text(
                text = activityLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!activityInfo.confirmationNumber.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.share_booking_ref, activityInfo.confirmationNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ShareErrorDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_error_title)) },
        text = { Text(stringResource(R.string.share_error_message)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_ok))
            }
        },
    )
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true)
@Composable
private fun ShareTripSelectionDialogPreview() {
    WanderVaultTheme {
        ShareTripSelectionDialog(
            documentName = "boarding_pass.pdf",
            trips = listOf(
                Trip(id = 1, title = "Summer in Italy", startDate = LocalDate.of(2025, 7, 1)),
                Trip(id = 2, title = "Tokyo Adventure"),
                Trip(id = 3, title = "Weekend in Paris"),
            ),
            onTripSelected = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareFlightLegSelectionDialogPreview() {
    WanderVaultTheme {
        ShareFlightLegSelectionDialog(
            flightInfo = FlightInfo(
                airline = "Ryanair",
                flightNumber = "FR1234",
                bookingReference = "ABCDEF",
                departurePlace = "BCN",
                arrivalPlace = "CDG",
            ),
            candidates = listOf(
                TransportLeg(id = 1, transportId = 1, type = TransportType.FLIGHT, company = "Ryanair", flightNumber = "FR1234"),
                TransportLeg(id = 2, transportId = 2, type = TransportType.FLIGHT, company = "Vueling", flightNumber = "VY456"),
            ),
            onLegSelected = {},
            onSkip = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareHotelSelectionDialogPreview() {
    WanderVaultTheme {
        ShareHotelDestinationSelectionDialog(
            hotelInfo = HotelInfo(
                name = "Hotel Colosseum",
                address = "Via Sacra 1, Rome",
                bookingReference = "HTL-99887",
            ),
            candidates = listOf(
                Destination(id = 1, tripId = 1, name = "Rome", position = 0),
                Destination(id = 2, tripId = 1, name = "Florence", position = 1),
            ),
            onDestinationSelected = {},
            onSkip = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareFlightConfirmDialogPreview() {
    WanderVaultTheme {
        ShareFlightConfirmDialog(
            flightInfo = FlightInfo(
                airline = "Ryanair",
                flightNumber = "FR1234",
                bookingReference = "ABCDEF",
                departurePlace = "BCN",
                arrivalPlace = "CDG",
            ),
            matchedLeg = TransportLeg(
                id = 1,
                transportId = 1,
                type = TransportType.FLIGHT,
                company = "Ryanair",
                flightNumber = "FR1234",
                reservationConfirmationNumber = "ABCDEF",
                stopName = "Paris CDG",
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareHotelConfirmDialogPreview() {
    WanderVaultTheme {
        ShareHotelConfirmDialog(
            hotelInfo = HotelInfo(
                name = "Hotel Colosseum",
                address = "Via Sacra 1, Rome",
                bookingReference = "HTL-99887",
            ),
            destination = Destination(
                id = 1,
                tripId = 1,
                name = "Rome",
                position = 0,
                arrivalDateTime = LocalDate.of(2025, 7, 10).atStartOfDay(ZoneId.systemDefault()),
                departureDateTime = LocalDate.of(2025, 7, 14).atStartOfDay(ZoneId.systemDefault()),
            ),
            existingHotel = Hotel(
                destinationId = 1,
                name = "Hotel Colosseum",
                address = "",
                reservationNumber = "HTL-99887",
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareActivitySelectionDialogPreview() {
    WanderVaultTheme {
        ShareActivityDestinationSelectionDialog(
            activityInfo = ActivityInfo(
                title = "Eiffel Tower tour",
                confirmationNumber = "ACT-12345",
            ),
            candidates = listOf(
                Destination(id = 1, tripId = 1, name = "Paris", position = 0),
                Destination(id = 2, tripId = 1, name = "Lyon", position = 1),
            ),
            onDestinationSelected = {},
            onSkip = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareActivityConfirmDialogPreview() {
    WanderVaultTheme {
        ShareActivityConfirmDialog(
            activityInfo = ActivityInfo(
                title = "Eiffel Tower tour",
                confirmationNumber = "ACT-12345",
            ),
            destination = Destination(
                id = 1,
                tripId = 1,
                name = "Paris",
                position = 0,
                arrivalDateTime = LocalDate.of(2025, 7, 10).atStartOfDay(ZoneId.systemDefault()),
                departureDateTime = LocalDate.of(2025, 7, 14).atStartOfDay(ZoneId.systemDefault()),
            ),
            onConfirm = {},
            onDismiss = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareErrorDialogPreview() {
    WanderVaultTheme {
        ShareErrorDialog(onDismiss = {})
    }
}
