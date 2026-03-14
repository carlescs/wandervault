package cat.company.wandervault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.company.wandervault.R
import java.time.ZoneId

/** All available IANA timezone IDs, sorted alphabetically. Computed once at class-load time. */
private val ALL_TIMEZONES: List<String> = ZoneId.getAvailableZoneIds().sorted()

/**
 * A dialog that lets the user pick a timezone from all available IANA timezone IDs.
 *
 * The dialog includes a search field that filters the list by name. The device's current
 * default timezone is always shown as the first option (equivalent to clearing an explicit
 * selection and following the device setting).
 *
 * @param onTimezoneSelected Called with the selected IANA zone ID, or `null` for device default.
 * @param onDismiss Called when the dialog is dismissed without selecting a timezone.
 */
@Composable
internal fun TimezonePickerDialog(
    onTimezoneSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val deviceDefault = remember { ZoneId.systemDefault().id }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredTimezones = remember(searchQuery, deviceDefault) {
        val base = if (searchQuery.isBlank()) {
            ALL_TIMEZONES
        } else {
            ALL_TIMEZONES.filter { it.contains(searchQuery, ignoreCase = true) }
        }
        // Exclude the device-default entry from the regular list; it has its own row at the top.
        base.filter { it != deviceDefault }
    }
    val showDeviceDefault = searchQuery.isBlank() ||
        deviceDefault.contains(searchQuery, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.itinerary_pick_timezone)) },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.timezone_search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    if (showDeviceDefault) {
                        item(key = "__device_default__") {
                            Text(
                                text = stringResource(R.string.trip_timezone_device_default, deviceDefault),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTimezoneSelected(null) }
                                    .padding(vertical = 12.dp),
                            )
                            HorizontalDivider()
                        }
                    }
                    items(filteredTimezones, key = { it }) { zoneId ->
                        Text(
                            text = zoneId,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onTimezoneSelected(zoneId) }
                                .padding(vertical = 12.dp),
                        )
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
