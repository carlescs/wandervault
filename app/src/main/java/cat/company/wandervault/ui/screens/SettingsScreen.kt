package cat.company.wandervault.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cat.company.wandervault.R
import cat.company.wandervault.ui.theme.WanderVaultTheme
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.util.Locale

/** BCP-47 language tags available for AI-generated content. */
internal val AI_LANGUAGE_TAGS = listOf(
    "ar",
    "de",
    "en",
    "es",
    "fr",
    "it",
    "ja",
    "ko",
    "nl",
    "pt",
    "ru",
    "zh",
)

/**
 * Settings screen.
 *
 * Displays app-wide settings grouped into sections. Exposes:
 * - An app-wide default timezone selector under the "General" section.
 * - An AI language selector under the "AI" section.
 * - A "Data Administration" entry that opens the backup/restore flow.
 *
 * @param onNavigateUp Called when the user taps the back button.
 * @param onNavigateToDataAdmin Called when the user taps the Data Administration row.
 * @param modifier Optional [Modifier].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateUp: () -> Unit,
    onNavigateToDataAdmin: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsContent(
        uiState = uiState,
        onNavigateUp = onNavigateUp,
        onNavigateToDataAdmin = onNavigateToDataAdmin,
        onDefaultTimezoneChange = viewModel::onDefaultTimezoneChange,
        onAiLanguageChange = viewModel::onAiLanguageChange,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
    uiState: SettingsUiState,
    onNavigateUp: () -> Unit,
    onNavigateToDataAdmin: () -> Unit,
    onDefaultTimezoneChange: (String?) -> Unit = {},
    onAiLanguageChange: (String?) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_navigate_up),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── General section ────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_section_general),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            AppTimezoneRow(
                currentTimezone = uiState.defaultTimezone,
                onTimezoneChange = onDefaultTimezoneChange,
            )
            HorizontalDivider()

            // ── AI section ─────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_section_ai),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            AiLanguageRow(
                currentLanguageTag = uiState.aiLanguage,
                onLanguageChange = onAiLanguageChange,
            )
            HorizontalDivider()

            // ── Data section ───────────────────────────────────────────────
            Text(
                text = stringResource(R.string.settings_section_data),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            HorizontalDivider()
            // Data Administration row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onNavigateToDataAdmin)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Storage,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_data_admin_title),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_data_admin_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            HorizontalDivider()
        }
    }
}

/**
 * A settings row that lets the user pick the app-wide default timezone from a dialog.
 *
 * The device's current default timezone is always shown as the "no preference" option.
 */
@Composable
private fun AppTimezoneRow(
    currentTimezone: String?,
    onTimezoneChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deviceDefault = remember { ZoneId.systemDefault().id }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val displayValue = currentTimezone ?: stringResource(R.string.trip_timezone_device_default, deviceDefault)

    if (showPicker) {
        TimezonePickerDialog(
            onTimezoneSelected = { zoneId ->
                onTimezoneChange(zoneId)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.weight(1f)) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_default_timezone_label)) },
                trailingIcon = {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .semantics {
                        role = Role.Button
                        contentDescription = displayValue
                    }
                    .clickable { showPicker = true },
            )
        }
    }
}

/**
 * A settings row that lets the user pick the preferred language for AI-generated content
 * using a combo box.
 *
 * The device's current default language is always shown as the "no preference" option.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiLanguageRow(
    currentLanguageTag: String?,
    onLanguageChange: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val deviceLocale = LocalConfiguration.current.locales.get(0) ?: Locale.getDefault()
    val deviceDefaultLanguage = deviceLocale.getDisplayLanguage(deviceLocale)
        .replaceFirstChar { it.titlecase(deviceLocale) }
    val deviceDefaultLabel = stringResource(
        R.string.settings_ai_language_device_default,
        deviceDefaultLanguage,
    )
    val displayValue = if (currentLanguageTag != null) {
        Locale.forLanguageTag(currentLanguageTag).getDisplayLanguage(deviceLocale)
            .replaceFirstChar { it.titlecase(deviceLocale) }
    } else {
        deviceDefaultLabel
    }

    var expanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
        Spacer(modifier = Modifier.width(16.dp))
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = displayValue,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.settings_ai_language_label)) },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(deviceDefaultLabel) },
                    onClick = {
                        onLanguageChange(null)
                        expanded = false
                    },
                )
                AI_LANGUAGE_TAGS.forEach { tag ->
                    val label = Locale.forLanguageTag(tag)
                        .getDisplayLanguage(deviceLocale)
                        .replaceFirstChar { it.titlecase(deviceLocale) }
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onLanguageChange(tag)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    WanderVaultTheme {
        SettingsContent(
            uiState = SettingsUiState(defaultTimezone = "Europe/London", aiLanguage = "en"),
            onNavigateUp = {},
            onNavigateToDataAdmin = {},
        )
    }
}
