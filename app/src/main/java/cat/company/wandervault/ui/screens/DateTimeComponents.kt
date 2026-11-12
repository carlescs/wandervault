package cat.company.wandervault.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cat.company.wandervault.R
import cat.company.wandervault.ui.theme.WanderVaultTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Number of milliseconds in a day, used to convert between [LocalDate] and [DatePicker] millis. */
internal const val MILLIS_PER_DAY = 86_400_000L

/** Converts a [ZonedDateTime] to epoch-day milliseconds used by the [DatePicker] API. */
internal fun ZonedDateTime?.toDateEpochMillis(): Long? =
    this?.toLocalDate()?.toEpochDay()?.times(MILLIS_PER_DAY)

/**
 * A labelled date / time / timezone editor for a single [ZonedDateTime] value.
 *
 * Renders the [label] above a row of three compact picker chips:
 * - **Date** opens a [DatePickerDialog] constrained to [minDateMillis]..[maxDateMillis].
 * - **Time** opens a [TimeSelectionDialog] (requires a date to be set first).
 * - **Timezone** opens a [TimezonePickerDialog] (requires a date to be set first).
 *
 * Selecting a new date preserves the existing time (or defaults to midnight) and zone. Changing
 * the timezone keeps the same local date and time; only the zone offset changes.
 *
 * @param label Short caption shown above the chips (e.g. "Arrival").
 * @param dateTime The current value, or `null` when not set yet.
 * @param onDateTimeChange Called with the updated value.
 * @param minDateMillis Optional inclusive lower bound for selectable dates (epoch-day millis).
 * @param maxDateMillis Optional inclusive upper bound for selectable dates (epoch-day millis).
 * @param defaultZoneId Zone used when a date is picked for an unset value.
 * @param labelIcon Optional icon shown before the [label].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateTimeField(
    label: String,
    dateTime: ZonedDateTime?,
    onDateTimeChange: (ZonedDateTime?) -> Unit,
    modifier: Modifier = Modifier,
    minDateMillis: Long? = null,
    maxDateMillis: Long? = null,
    defaultZoneId: ZoneId = ZoneId.systemDefault(),
    labelIcon: ImageVector? = null,
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var showTimezonePicker by rememberSaveable { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM) }
    val timeFormatter = remember { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT) }

    if (showDatePicker) {
        // Normalize bounds once so an inverted range (e.g. from out-of-order existing data)
        // doesn't disable all dates and lock the user out of correcting them.
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
        // When no date is selected yet, open the picker at the month of the nearest contextual
        // bound so the user sees a relevant month instead of today, which may be far from the
        // trip dates.
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
                    enabled = state.selectedDateMillis != null,
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
        TimeSelectionDialog(
            title = label,
            initialHour = dateTime?.hour ?: 0,
            initialMinute = dateTime?.minute ?: 0,
            onConfirm = { hour, minute ->
                // dateTime is always non-null here: the time chip is disabled when
                // dateTime == null, so this branch is purely defensive.
                val date = dateTime?.toLocalDate() ?: LocalDate.now(defaultZoneId)
                val zone = dateTime?.zone ?: defaultZoneId
                onDateTimeChange(ZonedDateTime.of(date, LocalTime.of(hour, minute), zone))
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }

    if (showTimezonePicker) {
        TimezonePickerDialog(
            selectedZoneId = dateTime?.zone?.id,
            onTimezoneSelected = { zoneId ->
                val zone = if (zoneId != null) ZoneId.of(zoneId) else ZoneId.systemDefault()
                dateTime?.let { onDateTimeChange(it.withZoneSameLocal(zone)) }
                showTimezonePicker = false
            },
            onDismiss = { showTimezonePicker = false },
        )
    }

    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (labelIcon != null) {
                Icon(
                    imageVector = labelIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PickerChip(
                text = dateTime?.toLocalDate()?.format(dateFormatter)
                    ?: stringResource(R.string.itinerary_pick_date),
                icon = Icons.Outlined.CalendarMonth,
                onClick = { showDatePicker = true },
                isPlaceholder = dateTime == null,
                modifier = Modifier.weight(1f),
            )
            PickerChip(
                text = dateTime?.format(timeFormatter) ?: "--:--",
                icon = Icons.Outlined.Schedule,
                onClick = { showTimePicker = true },
                enabled = dateTime != null,
                isPlaceholder = dateTime == null,
            )
            PickerChip(
                text = dateTime?.zone?.getDisplayName(TextStyle.SHORT, Locale.getDefault()) ?: "---",
                icon = Icons.Outlined.Public,
                onClick = { showTimezonePicker = true },
                enabled = dateTime != null,
                isPlaceholder = dateTime == null,
            )
        }
    }
}

/**
 * A compact, rounded, outlined button with a leading [icon], used for the date / time / zone
 * selectors in [DateTimeField].
 *
 * @param isPlaceholder Renders [text] in a muted colour when no value has been set yet.
 */
@Composable
private fun PickerChip(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isPlaceholder: Boolean = false,
) {
    val disabledAlpha = 0.38f
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha)
        isPlaceholder -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    val iconColor = if (enabled) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = disabledAlpha)
    }
    val borderColor = if (enabled) {
        MaterialTheme.colorScheme.outlineVariant
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = disabledAlpha)
    }
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, borderColor),
        modifier = modifier.height(36.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Material 3 time-picker dialog with a toggle between the clock dial and keyboard input.
 *
 * @param title Caption shown at the top of the dialog.
 * @param initialHour Hour (0-23) shown when the dialog opens.
 * @param initialMinute Minute (0-59) shown when the dialog opens.
 * @param onConfirm Called with the selected hour and minute.
 * @param onDismiss Called when the dialog is dismissed without confirming.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeSelectionDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute)
    var showDial by rememberSaveable { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                )
                if (showDial) {
                    TimePicker(state = state)
                } else {
                    TimeInput(state = state)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { showDial = !showDial }) {
                        Icon(
                            imageVector = if (showDial) Icons.Outlined.Keyboard else Icons.Outlined.Schedule,
                            contentDescription = stringResource(
                                if (showDial) R.string.time_picker_switch_to_input else R.string.time_picker_switch_to_dial,
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                        Text(stringResource(R.string.dialog_ok))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DateTimeFieldPreview() {
    WanderVaultTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DateTimeField(
                label = "Arrival",
                dateTime = ZonedDateTime.of(2024, 6, 1, 12, 30, 0, 0, ZoneId.of("Europe/Paris")),
                onDateTimeChange = {},
            )
            DateTimeField(label = "Departure", dateTime = null, onDateTimeChange = {})
        }
    }
}
