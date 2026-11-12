package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cat.company.wandervault.R
import cat.company.wandervault.ui.theme.WanderVaultTheme
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** All available IANA timezone IDs, sorted alphabetically. Computed once at class-load time. */
private val ALL_TIMEZONES: List<String> = ZoneId.getAvailableZoneIds().sorted()

/**
 * A dialog that lets the user pick a timezone from all available IANA timezone IDs.
 *
 * Each zone is shown with a readable city name as headline and its region and current UTC
 * offset underneath. The dialog includes a search field that filters the list by zone ID or
 * city name. The device's current default timezone is always shown as the first option
 * (equivalent to clearing an explicit selection and following the device setting).
 *
 * @param onTimezoneSelected Called with the selected IANA zone ID, or `null` for device default.
 * @param onDismiss Called when the dialog is dismissed without selecting a timezone.
 * @param selectedZoneId The currently selected zone ID, highlighted with a check mark, or `null`
 *   when no explicit zone is selected.
 * @param isDeviceDefaultSelected Whether the "device default" option should be highlighted.
 */
@Composable
internal fun TimezonePickerDialog(
    onTimezoneSelected: (String?) -> Unit,
    onDismiss: () -> Unit,
    selectedZoneId: String? = null,
    isDeviceDefaultSelected: Boolean = false,
) {
    val deviceDefault = remember { ZoneId.systemDefault().id }
    val now = remember { Instant.now() }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredTimezones = remember(searchQuery, deviceDefault) {
        val base = if (searchQuery.isBlank()) {
            ALL_TIMEZONES
        } else {
            ALL_TIMEZONES.filter { it.matchesTimezoneQuery(searchQuery) }
        }
        // Exclude the device-default entry from the regular list; it has its own row at the top.
        base.filter { it != deviceDefault }
    }
    val showDeviceDefault = searchQuery.isBlank() || deviceDefault.matchesTimezoneQuery(searchQuery)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Public, contentDescription = null) },
        title = { Text(stringResource(R.string.itinerary_pick_timezone)) },
        text = {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = stringResource(R.string.timezone_search_hint),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = stringResource(R.string.timezone_search_clear),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    if (showDeviceDefault) {
                        item(key = "__device_default__") {
                            TimezoneRow(
                                headline = stringResource(R.string.timezone_device_default_short),
                                supporting = "${deviceDefault.timezoneCity()} · ${deviceDefault.utcOffsetLabel(now)}",
                                selected = isDeviceDefaultSelected || selectedZoneId == deviceDefault,
                                leadingIcon = Icons.Outlined.MyLocation,
                                onClick = { onTimezoneSelected(null) },
                            )
                        }
                    }
                    items(filteredTimezones, key = { it }) { zoneId ->
                        TimezoneRow(
                            headline = zoneId.timezoneCity(),
                            supporting = listOfNotNull(zoneId.timezoneRegion(), zoneId.utcOffsetLabel(now))
                                .joinToString(" · "),
                            selected = zoneId == selectedZoneId,
                            onClick = { onTimezoneSelected(zoneId) },
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

/** A single selectable timezone entry: headline, supporting line and an optional check mark. */
@Composable
private fun TimezoneRow(
    headline: String,
    supporting: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** True when this zone ID matches [query] by ID or by its readable city name. */
private fun String.matchesTimezoneQuery(query: String): Boolean {
    val trimmed = query.trim()
    return contains(trimmed, ignoreCase = true) || timezoneCity().contains(trimmed, ignoreCase = true)
}

/** Readable city name for an IANA zone ID, e.g. `America/Argentina/Buenos_Aires` → `Buenos Aires`. */
private fun String.timezoneCity(): String = substringAfterLast('/').replace('_', ' ')

/** Region part of an IANA zone ID, e.g. `America/Argentina/Buenos_Aires` → `America / Argentina`. */
private fun String.timezoneRegion(): String? =
    if (contains('/')) substringBeforeLast('/').replace('_', ' ').replace("/", " / ") else null

/** Current UTC offset label for an IANA zone ID, e.g. `UTC+01:00`, or `UTC` for a zero offset. */
private fun String.utcOffsetLabel(at: Instant): String {
    val offset = runCatching { ZoneId.of(this).rules.getOffset(at) }.getOrNull() ?: return ""
    return if (offset == ZoneOffset.UTC) "UTC" else "UTC${offset.id}"
}

@Preview(showBackground = true)
@Composable
private fun TimezonePickerDialogPreview() {
    WanderVaultTheme {
        TimezonePickerDialog(
            onTimezoneSelected = {},
            onDismiss = {},
            selectedZoneId = "Europe/Paris",
        )
    }
}
