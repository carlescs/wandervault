package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalActivity
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
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

    val dialogIcon = when (analyzeState) {
        is AnalyzeDocumentUiState.FlightLegSelection,
        is AnalyzeDocumentUiState.FlightTransportSelection,
        is AnalyzeDocumentUiState.FlightConfirm,
        is AnalyzeDocumentUiState.FlightAddLegConfirm,
        -> Icons.Outlined.Flight
        is AnalyzeDocumentUiState.HotelDestinationSelection,
        is AnalyzeDocumentUiState.HotelConfirm,
        -> Icons.Outlined.Hotel
        is AnalyzeDocumentUiState.ActivityDestinationSelection,
        is AnalyzeDocumentUiState.ActivityConfirm,
        -> Icons.Outlined.LocalActivity
        is AnalyzeDocumentUiState.TripInfoConfirm -> Icons.Outlined.Info
        is AnalyzeDocumentUiState.Error -> Icons.Outlined.ErrorOutline
        else -> Icons.Outlined.AutoAwesome
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            DialogIcon(
                icon = dialogIcon,
                isDestructive = analyzeState is AnalyzeDocumentUiState.Error,
            )
        },
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .then(if (useScroll) Modifier.verticalScroll(scrollState) else Modifier),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (analyzeState) {
                    is AnalyzeDocumentUiState.Unavailable -> {
                        DialogSupportingText(text = stringResource(R.string.documents_analyze_unavailable))
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
                        DialogSupportingText(text = stringResource(R.string.documents_analyze_confirm_flight_message))
                        FlightLegListItem(leg = analyzeState.matchedLeg, onClick = null)
                    }

                    is AnalyzeDocumentUiState.HotelConfirm -> {
                        AnalyzeHotelInfoSummary(hotelInfo = analyzeState.hotelInfo)
                        DialogSupportingText(
                            text = stringResource(
                                if (analyzeState.existingHotel != null) {
                                    R.string.documents_analyze_confirm_hotel_message
                                } else {
                                    R.string.documents_analyze_confirm_hotel_new_message
                                },
                                analyzeState.destination.name,
                            ),
                        )
                        DestinationListItem(
                            destination = analyzeState.destination,
                            dateFormatter = dateFormatter,
                            onClick = null,
                            extraDetail = analyzeState.existingHotel?.name?.ifBlank { null },
                        )
                    }

                    is AnalyzeDocumentUiState.FlightLegSelection -> {
                        AnalyzeFlightInfoSummary(flightInfo = analyzeState.flightInfo)
                        DialogSupportingText(text = stringResource(R.string.share_flight_selection_message))
                        DialogSelectionList(options = analyzeState.candidates) { leg ->
                            FlightLegListItem(leg = leg, onClick = { onFlightLegSelected(leg) })
                        }
                    }

                    is AnalyzeDocumentUiState.FlightTransportSelection -> {
                        AnalyzeFlightInfoSummary(flightInfo = analyzeState.flightInfo)
                        DialogSupportingText(
                            text = stringResource(R.string.documents_analyze_transport_selection_message),
                        )
                        DialogSelectionList(options = analyzeState.candidates) { destination ->
                            DestinationListItem(
                                destination = destination,
                                dateFormatter = dateFormatter,
                                onClick = { onFlightTransportSelected(destination) },
                            )
                        }
                    }

                    is AnalyzeDocumentUiState.FlightAddLegConfirm -> {
                        AnalyzeFlightInfoSummary(flightInfo = analyzeState.flightInfo)
                        DialogSupportingText(
                            text = stringResource(
                                R.string.documents_analyze_confirm_add_leg_message,
                                analyzeState.destination.name,
                            ),
                        )
                        DestinationListItem(
                            destination = analyzeState.destination,
                            dateFormatter = dateFormatter,
                            onClick = null,
                        )
                    }

                    is AnalyzeDocumentUiState.HotelDestinationSelection -> {
                        AnalyzeHotelInfoSummary(hotelInfo = analyzeState.hotelInfo)
                        DialogSupportingText(text = stringResource(R.string.share_hotel_selection_message))
                        DialogSelectionList(options = analyzeState.candidates) { destination ->
                            DestinationListItem(
                                destination = destination,
                                dateFormatter = dateFormatter,
                                onClick = { onHotelDestinationSelected(destination) },
                            )
                        }
                    }

                    is AnalyzeDocumentUiState.TripInfoConfirm -> {
                        DialogSupportingText(text = stringResource(R.string.documents_analyze_confirm_trip_info_message))
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = analyzeState.relevantTripInfo,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp),
                            )
                        }
                    }

                    is AnalyzeDocumentUiState.ActivityDestinationSelection -> {
                        AnalyzeActivityInfoSummary(activityInfo = analyzeState.activityInfo)
                        DialogSupportingText(
                            text = stringResource(R.string.documents_analyze_activity_selection_message),
                        )
                        DialogSelectionList(options = analyzeState.candidates) { destination ->
                            DestinationListItem(
                                destination = destination,
                                dateFormatter = dateFormatter,
                                onClick = { onActivityDestinationSelected(destination) },
                            )
                        }
                    }

                    is AnalyzeDocumentUiState.ActivityConfirm -> {
                        AnalyzeActivityInfoSummary(activityInfo = analyzeState.activityInfo)
                        DialogSupportingText(
                            text = stringResource(
                                R.string.documents_analyze_confirm_activity_message,
                                analyzeState.destination.name,
                            ),
                        )
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

/**
 * Scrollable list of selectable options inside an analysis / share dialog. Uses a bounded height
 * so the dialog's summary and buttons stay visible with long candidate lists.
 */
@Composable
internal fun <T> DialogSelectionList(
    options: List<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (T) -> Unit,
) {
    LazyColumn(
        modifier = modifier.heightIn(max = 280.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { option -> itemContent(option) }
    }
}

/** Summary card for flight details extracted from a document. */
@Composable
internal fun AnalyzeFlightInfoSummary(flightInfo: FlightInfo, modifier: Modifier = Modifier) {
    DialogSummaryCard(
        icon = Icons.Outlined.Flight,
        title = listOfNotNull(flightInfo.airline, flightInfo.flightNumber)
            .joinToString(" ")
            .ifBlank { stringResource(R.string.share_unknown_flight) },
        details = listOf(
            flightInfo.bookingReference?.ifBlank { null }
                ?.let { stringResource(R.string.documents_analyze_ref, it) },
        ),
        modifier = modifier,
    )
}

/** Summary card for hotel details extracted from a document. */
@Composable
internal fun AnalyzeHotelInfoSummary(hotelInfo: HotelInfo, modifier: Modifier = Modifier) {
    DialogSummaryCard(
        icon = Icons.Outlined.Hotel,
        title = hotelInfo.name?.ifBlank { null } ?: stringResource(R.string.share_unknown_hotel),
        details = listOf(
            hotelInfo.bookingReference?.ifBlank { null }
                ?.let { stringResource(R.string.documents_analyze_ref, it) },
        ),
        modifier = modifier,
    )
}

/** Summary card for activity details extracted from a document. */
@Composable
internal fun AnalyzeActivityInfoSummary(activityInfo: ActivityInfo, modifier: Modifier = Modifier) {
    DialogSummaryCard(
        icon = Icons.Outlined.LocalActivity,
        title = activityInfo.title?.ifBlank { null }
            ?: stringResource(R.string.documents_analyze_activity_unknown),
        details = listOf(
            activityInfo.description,
            activityInfo.confirmationNumber?.ifBlank { null }
                ?.let { stringResource(R.string.documents_analyze_ref, it) },
        ),
        modifier = modifier,
    )
}

/**
 * A transport leg rendered inside a selection or confirmation dialog: company + flight number
 * as the title, with the booking reference and intermediate stop underneath.
 *
 * When [onClick] is non-null the row is tappable; otherwise it is a static preview.
 */
@Composable
internal fun FlightLegListItem(
    leg: TransportLeg,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    DialogOptionItem(
        icon = Icons.Outlined.Flight,
        title = listOfNotNull(leg.company, leg.flightNumber)
            .joinToString(" ")
            .ifBlank { stringResource(R.string.share_unnamed_flight_leg) },
        details = listOf(
            leg.reservationConfirmationNumber?.ifBlank { null }
                ?.let { stringResource(R.string.documents_analyze_ref, it) },
            leg.stopName?.ifBlank { null }
                ?.let { stringResource(R.string.documents_analyze_to, it) },
        ),
        onClick = onClick,
        modifier = modifier,
    )
}

/**
 * A single destination row rendered inside a selection or confirmation dialog.
 *
 * When [onClick] is non-null the row is tappable (single-line name, trailing chevron). When
 * [onClick] is null the row is a static preview of the destination that will be updated.
 *
 * @param extraDetail Optional additional line shown under the stay dates (e.g. the hotel name).
 */
@Composable
internal fun DestinationListItem(
    destination: Destination,
    dateFormatter: DateTimeFormatter,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    extraDetail: String? = null,
) {
    DialogOptionItem(
        icon = Icons.Outlined.Place,
        title = destination.name,
        details = listOf(destination.stayDateRange(dateFormatter), extraDetail),
        onClick = onClick,
        titleMaxLines = if (onClick != null) 1 else 3,
        modifier = modifier,
    )
}

/**
 * Formats the destination's stay as "arrival – departure" (or a single date when only one is
 * known), or returns `null` when neither date is set.
 */
internal fun Destination.stayDateRange(dateFormatter: DateTimeFormatter): String? {
    val arrival = arrivalDateTime?.toLocalDate()
    val departure = departureDateTime?.toLocalDate()
    return when {
        arrival != null && departure != null ->
            "${arrival.format(dateFormatter)} – ${departure.format(dateFormatter)}"
        arrival != null -> arrival.format(dateFormatter)
        departure != null -> departure.format(dateFormatter)
        else -> null
    }
}
