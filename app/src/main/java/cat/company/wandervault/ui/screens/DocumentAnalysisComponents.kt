package cat.company.wandervault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.company.wandervault.R
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.DocumentExtractionResult
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.TransportLeg
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A single unified dialog that covers the entire document-analysis flow. Keeping one [AlertDialog]
 * composable alive for the lifetime of the analysis (rather than swapping between separate
 * composables per state) prevents a subtle Android bug: removing a Compose [AlertDialog] from the
 * composition calls [android.app.Dialog.dismiss] on the underlying window, which asynchronously
 * fires the [android.content.DialogInterface.OnDismissListener] and therefore also invokes
 * [onDismissRequest]. If a separate dialog were used for each state the transition
 * Result → FlightConfirm / HotelConfirm / TripInfoConfirm would silently invoke [onDismiss],
 * clearing the current [AnalyzeDocumentUiState] and preventing the confirmation dialog from being
 * displayed.
 *
 * The dialog title, body, confirm button and dismiss button all adapt to [analyzeState]:
 * - [AnalyzeDocumentUiState.Loading] / [AnalyzeDocumentUiState.Downloading]: progress indicator.
 * - [AnalyzeDocumentUiState.Result]: summary + optional "Apply Changes" button.
 * - [AnalyzeDocumentUiState.Unavailable] / [AnalyzeDocumentUiState.Error]: status message.
 * - [AnalyzeDocumentUiState.FlightConfirm] / [AnalyzeDocumentUiState.HotelConfirm]: extracted
 *   info alongside the matched leg / destination; "Confirm" button applies the changes.
 * - [AnalyzeDocumentUiState.TripInfoConfirm]: extracted general trip info text; "Apply" button
 *   saves it as the trip description.
 * - [AnalyzeDocumentUiState.FlightLegSelection] / [AnalyzeDocumentUiState.HotelDestinationSelection]:
 *   scrollable candidate list; tapping an item applies the changes and dismisses.
 */
@Composable
internal fun AnalyzeDocumentDialog(
    analyzeState: AnalyzeDocumentUiState,
    onApplyChanges: () -> Unit,
    onFlightLegSelected: (TransportLeg) -> Unit,
    onFlightConfirmed: () -> Unit,
    onHotelDestinationSelected: (Destination) -> Unit,
    onHotelConfirmed: () -> Unit,
    onTripInfoConfirmed: () -> Unit,
    onDismiss: () -> Unit,
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    val titleRes = when (analyzeState) {
        is AnalyzeDocumentUiState.FlightLegSelection -> R.string.share_flight_selection_title
        is AnalyzeDocumentUiState.HotelDestinationSelection -> R.string.share_hotel_selection_title
        is AnalyzeDocumentUiState.FlightConfirm -> R.string.documents_analyze_confirm_flight_title
        is AnalyzeDocumentUiState.HotelConfirm -> R.string.documents_analyze_confirm_hotel_title
        is AnalyzeDocumentUiState.TripInfoConfirm -> R.string.documents_analyze_confirm_trip_info_title
        else -> R.string.documents_analyze_title
    }

    // FlightLegSelection and HotelDestinationSelection use an inner LazyColumn which must not be
    // nested inside a verticalScroll container.
    val scrollState = rememberScrollState()
    val useScroll = analyzeState !is AnalyzeDocumentUiState.FlightLegSelection &&
        analyzeState !is AnalyzeDocumentUiState.HotelDestinationSelection

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .then(if (useScroll) Modifier.verticalScroll(scrollState) else Modifier),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (analyzeState) {
                    is AnalyzeDocumentUiState.Loading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text(
                            text = stringResource(R.string.documents_analyze_analyzing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is AnalyzeDocumentUiState.Downloading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text(
                            text = stringResource(R.string.documents_analyze_downloading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (analyzeState.bytesDownloaded > 0) {
                            Text(
                                text = stringResource(
                                    R.string.documents_analyze_downloaded_bytes,
                                    formatBytes(analyzeState.bytesDownloaded),
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    is AnalyzeDocumentUiState.Unavailable -> {
                        Text(
                            text = stringResource(R.string.documents_analyze_unavailable),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    is AnalyzeDocumentUiState.Error -> {
                        Column(
                            modifier = Modifier.semantics(mergeDescendants = true) {},
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.documents_analyze_error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                            analyzeState.message?.let { errorDetail ->
                                Text(
                                    text = errorDetail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.Result -> {
                        val extraction = analyzeState.extractionResult
                        // Summary section
                        Text(
                            text = stringResource(R.string.documents_analyze_summary_label),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = extraction.summary.ifBlank {
                                stringResource(R.string.documents_analyze_no_summary)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        // Proposed changes section
                        val hasChanges = extraction.hasProposedChanges()
                        if (hasChanges) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.documents_analyze_proposed_changes_label),
                                style = MaterialTheme.typography.labelLarge,
                            )
                            extraction.flightInfo?.let { flight ->
                                AnalyzeInfoSection(
                                    label = stringResource(R.string.documents_analyze_flight_info_label),
                                    info = buildFlightInfoText(
                                        flight = flight,
                                        formattedFrom = flight.departurePlace?.let {
                                            stringResource(R.string.documents_analyze_from, it)
                                        },
                                        formattedTo = flight.arrivalPlace?.let {
                                            stringResource(R.string.documents_analyze_to, it)
                                        },
                                        formattedRef = flight.bookingReference?.let {
                                            stringResource(R.string.documents_analyze_ref, it)
                                        },
                                    ),
                                )
                            }
                            extraction.hotelInfo?.let { hotel ->
                                AnalyzeInfoSection(
                                    label = stringResource(R.string.documents_analyze_hotel_info_label),
                                    info = buildHotelInfoText(
                                        hotel = hotel,
                                        formattedRef = hotel.bookingReference?.let {
                                            stringResource(R.string.documents_analyze_ref, it)
                                        },
                                    ),
                                )
                            }
                            extraction.relevantTripInfo?.let { tripInfo ->
                                AnalyzeInfoSection(
                                    label = stringResource(R.string.documents_analyze_trip_info_label),
                                    info = tripInfo,
                                )
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.FlightConfirm -> {
                        AnalyzeFlightInfoSummary(flightInfo = analyzeState.flightInfo)
                        Text(
                            text = stringResource(R.string.documents_analyze_confirm_flight_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider()
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
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Column {
                                val legLabel = listOfNotNull(
                                    analyzeState.matchedLeg.company,
                                    analyzeState.matchedLeg.flightNumber,
                                ).joinToString(" ")
                                    .ifBlank { stringResource(R.string.share_unnamed_flight_leg) }
                                Text(
                                    text = legLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (!analyzeState.matchedLeg.reservationConfirmationNumber.isNullOrBlank()) {
                                    Text(
                                        text = stringResource(
                                            R.string.documents_analyze_ref,
                                            analyzeState.matchedLeg.reservationConfirmationNumber,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (!analyzeState.matchedLeg.stopName.isNullOrBlank()) {
                                    Text(
                                        text = stringResource(
                                            R.string.documents_analyze_to,
                                            analyzeState.matchedLeg.stopName,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.HotelConfirm -> {
                        AnalyzeHotelInfoSummary(hotelInfo = analyzeState.hotelInfo)
                        if (analyzeState.existingHotel != null) {
                            Text(
                                text = stringResource(
                                    R.string.documents_analyze_confirm_hotel_message,
                                    analyzeState.destination.name,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        } else {
                            Text(
                                text = stringResource(
                                    R.string.documents_analyze_confirm_hotel_new_message,
                                    analyzeState.destination.name,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
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
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Column {
                                Text(
                                    text = analyzeState.destination.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                val arrival = analyzeState.destination.arrivalDateTime?.toLocalDate()
                                val departure = analyzeState.destination.departureDateTime?.toLocalDate()
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
                                analyzeState.existingHotel?.name?.ifBlank { null }?.let { hotelName ->
                                    Text(
                                        text = hotelName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.FlightLegSelection -> {
                        AnalyzeFlightInfoSummary(flightInfo = analyzeState.flightInfo)
                        Text(
                            text = stringResource(R.string.share_flight_selection_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(analyzeState.candidates) { leg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onFlightLegSelected(leg) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AirplanemodeActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.padding(end = 12.dp),
                                    )
                                    Column {
                                        val label = listOfNotNull(leg.company, leg.flightNumber)
                                            .joinToString(" ")
                                            .ifBlank { stringResource(R.string.share_unnamed_flight_leg) }
                                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                                        if (!leg.reservationConfirmationNumber.isNullOrBlank()) {
                                            Text(
                                                text = stringResource(
                                                    R.string.documents_analyze_ref,
                                                    leg.reservationConfirmationNumber,
                                                ),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.HotelDestinationSelection -> {
                        AnalyzeHotelInfoSummary(hotelInfo = analyzeState.hotelInfo)
                        Text(
                            text = stringResource(R.string.share_hotel_selection_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(analyzeState.candidates) { destination ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onHotelDestinationSelected(destination) }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .padding(end = 12.dp),
                                    )
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
                                HorizontalDivider()
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.TripInfoConfirm -> {
                        Text(
                            text = stringResource(R.string.documents_analyze_confirm_trip_info_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider()
                        Text(
                            text = analyzeState.relevantTripInfo,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            when {
                analyzeState is AnalyzeDocumentUiState.Result &&
                    analyzeState.extractionResult.hasProposedChanges() -> {
                    TextButton(onClick = onApplyChanges) {
                        Text(stringResource(R.string.documents_analyze_apply_changes))
                    }
                }
                analyzeState is AnalyzeDocumentUiState.FlightConfirm -> {
                    TextButton(onClick = onFlightConfirmed) {
                        Text(stringResource(R.string.documents_analyze_confirm_apply))
                    }
                }
                analyzeState is AnalyzeDocumentUiState.HotelConfirm -> {
                    TextButton(onClick = onHotelConfirmed) {
                        Text(stringResource(R.string.documents_analyze_confirm_apply))
                    }
                }
                analyzeState is AnalyzeDocumentUiState.TripInfoConfirm -> {
                    TextButton(onClick = onTripInfoConfirmed) {
                        Text(stringResource(R.string.documents_analyze_confirm_apply))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                val labelRes = when (analyzeState) {
                    is AnalyzeDocumentUiState.FlightLegSelection,
                    is AnalyzeDocumentUiState.HotelDestinationSelection,
                    -> R.string.share_skip
                    else -> R.string.dialog_cancel
                }
                Text(stringResource(labelRes))
            }
        },
    )
}

@Composable
internal fun AnalyzeFlightInfoSummary(flightInfo: FlightInfo, modifier: Modifier = Modifier) {
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
            modifier = Modifier.padding(end = 8.dp),
        )
        Column {
            val flightLabel = listOfNotNull(flightInfo.airline, flightInfo.flightNumber)
                .joinToString(" ")
                .ifBlank { stringResource(R.string.share_unknown_flight) }
            Text(text = flightLabel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            if (!flightInfo.bookingReference.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.documents_analyze_ref, flightInfo.bookingReference),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun AnalyzeHotelInfoSummary(hotelInfo: HotelInfo, modifier: Modifier = Modifier) {
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
            modifier = Modifier.padding(end = 8.dp),
        )
        Column {
            val hotelLabel = hotelInfo.name?.ifBlank { null }
                ?: stringResource(R.string.share_unknown_hotel)
            Text(
                text = hotelLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!hotelInfo.bookingReference.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.documents_analyze_ref, hotelInfo.bookingReference),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun AnalyzeInfoSection(label: String, info: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = info,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun buildFlightInfoText(
    flight: FlightInfo,
    formattedFrom: String?,
    formattedTo: String?,
    formattedRef: String?,
): String =
    listOfNotNull(
        flight.airline,
        flight.flightNumber,
        formattedFrom,
        formattedTo,
        formattedRef,
    ).joinToString(" · ")

internal fun buildHotelInfoText(hotel: HotelInfo, formattedRef: String?): String =
    listOfNotNull(
        hotel.name,
        hotel.address,
        formattedRef,
    ).joinToString(" · ")

internal fun DocumentExtractionResult.hasProposedChanges(): Boolean =
    flightInfo != null || hotelInfo != null || relevantTripInfo != null

/**
 * Formats [bytes] as a human-readable file size string (B / KB / MB).
 * Used to display Gemini Nano model download progress in the analysis dialog.
 */
internal fun formatBytes(bytes: Long): String = when {
    bytes < BYTES_PER_KB -> "$bytes B"
    bytes < BYTES_PER_MB -> "${bytes / BYTES_PER_KB} KB"
    else -> "%.1f MB".format(bytes.toFloat() / BYTES_PER_MB.toFloat())
}

private const val BYTES_PER_KB = 1_024L
private const val BYTES_PER_MB = 1_024L * 1_024L
