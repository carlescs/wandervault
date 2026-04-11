package cat.company.wandervault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import cat.company.wandervault.domain.model.ActivityInfo
import cat.company.wandervault.domain.model.Destination
import cat.company.wandervault.domain.model.FlightInfo
import cat.company.wandervault.domain.model.HotelInfo
import cat.company.wandervault.domain.model.TransportLeg
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * A single unified dialog that covers the interactive steps of the document-analysis flow.
 * Keeping one [AlertDialog] composable alive for the lifetime of the interactive analysis (rather
 * than swapping between separate composables per state) prevents a subtle Android bug: removing a
 * Compose [AlertDialog] from the composition calls [android.app.Dialog.dismiss] on the underlying
 * window, which asynchronously fires the [android.content.DialogInterface.OnDismissListener] and
 * therefore also invokes [onDismissRequest]. If a separate dialog were used for each state,
 * transitions between confirmation states would silently invoke [onDismiss], clearing the current
 * [AnalyzeDocumentUiState] and preventing the confirmation dialog from being displayed.
 *
 * [AnalyzeDocumentUiState.Loading] and [AnalyzeDocumentUiState.Downloading] are NOT handled here;
 * they are shown inline in the screen instead. This dialog is only shown for interactive states.
 *
 * The dialog title, body, confirm button and dismiss button all adapt to [analyzeState]:
 * - [AnalyzeDocumentUiState.Unavailable] / [AnalyzeDocumentUiState.Error]: status message.
 * - [AnalyzeDocumentUiState.FlightConfirm] / [AnalyzeDocumentUiState.HotelConfirm]: extracted
 *   info alongside the matched leg / destination; "Confirm" button applies the changes.
 * - [AnalyzeDocumentUiState.FlightAddLegConfirm]: extracted flight info alongside the selected
 *   destination; "Apply" button adds a new leg to that transport.
 * - [AnalyzeDocumentUiState.TripInfoConfirm]: extracted general trip info text; "Apply" button
 *   saves it as the trip description.
 * - [AnalyzeDocumentUiState.FlightLegSelection] / [AnalyzeDocumentUiState.HotelDestinationSelection] /
 *   [AnalyzeDocumentUiState.ActivityDestinationSelection]: scrollable candidate list; tapping an
 *   item advances to [AnalyzeDocumentUiState.FlightConfirm],
 *   [AnalyzeDocumentUiState.HotelConfirm], or [AnalyzeDocumentUiState.ActivityConfirm]
 *   respectively. Tapping an item in [AnalyzeDocumentUiState.FlightTransportSelection] advances
 *   to [AnalyzeDocumentUiState.FlightAddLegConfirm].
 * - [AnalyzeDocumentUiState.ActivityConfirm]: extracted activity info alongside the selected
 *   destination; "Apply" button creates the new activity.
 *
 * @param onDismiss Called to cancel and close the entire dialog (back button, outside tap, or
 *   "Cancel" during [AnalyzeDocumentUiState.Error] or [AnalyzeDocumentUiState.Unavailable]).
 * @param onSkipItem Called when the user taps "Skip" or "Cancel" during a per-item step
 *   ([AnalyzeDocumentUiState.FlightLegSelection], [AnalyzeDocumentUiState.FlightTransportSelection],
 *   [AnalyzeDocumentUiState.HotelDestinationSelection], [AnalyzeDocumentUiState.FlightConfirm],
 *   [AnalyzeDocumentUiState.FlightAddLegConfirm], [AnalyzeDocumentUiState.HotelConfirm],
 *   [AnalyzeDocumentUiState.ActivityDestinationSelection], or [AnalyzeDocumentUiState.ActivityConfirm]).
 *   Advances to the next pending item rather than closing the dialog entirely.
 */
@Composable
internal fun AnalyzeDocumentDialog(
    analyzeState: AnalyzeDocumentUiState,
    onFlightLegSelected: (TransportLeg) -> Unit,
    onFlightConfirmed: () -> Unit,
    onFlightTransportSelected: (Destination) -> Unit,
    onFlightAddLegConfirmed: () -> Unit,
    onHotelDestinationSelected: (Destination) -> Unit,
    onHotelConfirmed: () -> Unit,
    onTripInfoConfirmed: () -> Unit,
    onActivityDestinationSelected: (Destination) -> Unit,
    onActivityConfirmed: () -> Unit,
    onDismiss: () -> Unit,
    onSkipItem: () -> Unit = onDismiss,
) {
    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }

    val titleRes = when (analyzeState) {
        is AnalyzeDocumentUiState.FlightLegSelection -> R.string.share_flight_selection_title
        is AnalyzeDocumentUiState.FlightTransportSelection -> R.string.documents_analyze_transport_selection_title
        is AnalyzeDocumentUiState.HotelDestinationSelection -> R.string.share_hotel_selection_title
        is AnalyzeDocumentUiState.FlightConfirm -> R.string.documents_analyze_confirm_flight_title
        is AnalyzeDocumentUiState.FlightAddLegConfirm -> R.string.documents_analyze_confirm_add_leg_title
        is AnalyzeDocumentUiState.HotelConfirm -> R.string.documents_analyze_confirm_hotel_title
        is AnalyzeDocumentUiState.TripInfoConfirm -> R.string.documents_analyze_confirm_trip_info_title
        is AnalyzeDocumentUiState.ActivityDestinationSelection -> R.string.documents_analyze_activity_selection_title
        is AnalyzeDocumentUiState.ActivityConfirm -> R.string.documents_analyze_confirm_activity_title
        else -> R.string.documents_analyze_title
    }

    // Selection states use an inner LazyColumn which must not be nested inside a verticalScroll container.
    val scrollState = rememberScrollState()
    val useScroll = analyzeState !is AnalyzeDocumentUiState.FlightLegSelection &&
        analyzeState !is AnalyzeDocumentUiState.FlightTransportSelection &&
        analyzeState !is AnalyzeDocumentUiState.HotelDestinationSelection &&
        analyzeState !is AnalyzeDocumentUiState.ActivityDestinationSelection

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

                    is AnalyzeDocumentUiState.FlightTransportSelection -> {
                        AnalyzeFlightInfoSummary(flightInfo = analyzeState.flightInfo)
                        Text(
                            text = stringResource(R.string.documents_analyze_transport_selection_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(analyzeState.candidates) { destination ->
                                DestinationListItem(
                                    destination = destination,
                                    dateFormatter = dateFormatter,
                                    onClick = { onFlightTransportSelected(destination) },
                                )
                                HorizontalDivider()
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.FlightAddLegConfirm -> {
                        AnalyzeFlightInfoSummary(flightInfo = analyzeState.flightInfo)
                        Text(
                            text = stringResource(
                                R.string.documents_analyze_confirm_add_leg_message,
                                analyzeState.destination.name,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider()
                        DestinationListItem(
                            destination = analyzeState.destination,
                            dateFormatter = dateFormatter,
                            onClick = null,
                        )
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
                                DestinationListItem(
                                    destination = destination,
                                    dateFormatter = dateFormatter,
                                    onClick = { onHotelDestinationSelected(destination) },
                                )
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

                    is AnalyzeDocumentUiState.ActivityDestinationSelection -> {
                        AnalyzeActivityInfoSummary(activityInfo = analyzeState.activityInfo)
                        Text(
                            text = stringResource(R.string.documents_analyze_activity_selection_message),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                            items(analyzeState.candidates) { destination ->
                                DestinationListItem(
                                    destination = destination,
                                    dateFormatter = dateFormatter,
                                    onClick = { onActivityDestinationSelected(destination) },
                                )
                                HorizontalDivider()
                            }
                        }
                    }

                    is AnalyzeDocumentUiState.ActivityConfirm -> {
                        AnalyzeActivityInfoSummary(activityInfo = analyzeState.activityInfo)
                        Text(
                            text = stringResource(
                                R.string.documents_analyze_confirm_activity_message,
                                analyzeState.destination.name,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HorizontalDivider()
                        DestinationListItem(
                            destination = analyzeState.destination,
                            dateFormatter = dateFormatter,
                            onClick = null,
                        )
                    }

                    is AnalyzeDocumentUiState.Downloading,
                    is AnalyzeDocumentUiState.Loading,
                    -> Unit
                }
            }
        },
        confirmButton = {
            when {
                analyzeState is AnalyzeDocumentUiState.FlightConfirm -> {
                    TextButton(onClick = onFlightConfirmed) {
                        Text(stringResource(R.string.documents_analyze_confirm_apply))
                    }
                }
                analyzeState is AnalyzeDocumentUiState.FlightAddLegConfirm -> {
                    TextButton(onClick = onFlightAddLegConfirmed) {
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
                analyzeState is AnalyzeDocumentUiState.ActivityConfirm -> {
                    TextButton(onClick = onActivityConfirmed) {
                        Text(stringResource(R.string.documents_analyze_confirm_apply))
                    }
                }
            }
        },
        dismissButton = {
            // "Skip" / "Cancel" in per-item states advances to the next pending item rather
            // than closing the entire dialog. All other states use a full cancel/dismiss.
            val isPerItemState = analyzeState is AnalyzeDocumentUiState.FlightLegSelection ||
                analyzeState is AnalyzeDocumentUiState.FlightTransportSelection ||
                analyzeState is AnalyzeDocumentUiState.HotelDestinationSelection ||
                analyzeState is AnalyzeDocumentUiState.FlightConfirm ||
                analyzeState is AnalyzeDocumentUiState.FlightAddLegConfirm ||
                analyzeState is AnalyzeDocumentUiState.HotelConfirm ||
                analyzeState is AnalyzeDocumentUiState.ActivityDestinationSelection ||
                analyzeState is AnalyzeDocumentUiState.ActivityConfirm
            TextButton(onClick = if (isPerItemState) onSkipItem else onDismiss) {
                val labelRes = when (analyzeState) {
                    is AnalyzeDocumentUiState.FlightLegSelection,
                    is AnalyzeDocumentUiState.FlightTransportSelection,
                    is AnalyzeDocumentUiState.HotelDestinationSelection,
                    is AnalyzeDocumentUiState.ActivityDestinationSelection,
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
internal fun AnalyzeActivityInfoSummary(activityInfo: ActivityInfo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp),
        )
        Column {
            val activityLabel = activityInfo.title?.ifBlank { null }
                ?: stringResource(R.string.documents_analyze_activity_unknown)
            Text(
                text = activityLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!activityInfo.description.isNullOrBlank()) {
                Text(
                    text = activityInfo.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!activityInfo.confirmationNumber.isNullOrBlank()) {
                Text(
                    text = stringResource(R.string.documents_analyze_ref, activityInfo.confirmationNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * A single destination row rendered inside a selection or confirmation dialog.
 *
 * When [onClick] is non-null the row is clickable and uses list-item styling (compact padding,
 * single-line name with ellipsis). When [onClick] is null the row is non-interactive and uses
 * confirmation styling (larger vertical padding, bold multi-line name).
 */
@Composable
internal fun DestinationListItem(
    destination: Destination,
    dateFormatter: DateTimeFormatter,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isListItem = onClick != null
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(
                vertical = if (isListItem) 4.dp else 10.dp,
                horizontal = if (isListItem) 0.dp else 4.dp,
            ),
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
                text = destination.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isListItem) null else FontWeight.Bold,
                maxLines = if (isListItem) 1 else Int.MAX_VALUE,
                overflow = if (isListItem) TextOverflow.Ellipsis else TextOverflow.Clip,
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
