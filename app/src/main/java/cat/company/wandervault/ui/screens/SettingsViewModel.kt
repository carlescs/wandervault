package cat.company.wandervault.ui.screens

import androidx.lifecycle.ViewModel
import cat.company.wandervault.domain.repository.AppPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds the current app-wide settings state and handles user interactions.
 *
 * @param appPreferences Repository for reading and persisting app-wide preferences.
 */
class SettingsViewModel(
    private val appPreferences: AppPreferencesRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            defaultTimezone = appPreferences.getDefaultTimezone(),
            aiLanguage = appPreferences.getAiLanguage(),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * Persists the selected [zoneId] as the app-wide default timezone.
     * Passing `null` reverts to the device default.
     */
    fun onDefaultTimezoneChange(zoneId: String?) {
        appPreferences.setDefaultTimezone(zoneId)
        _uiState.update { it.copy(defaultTimezone = zoneId) }
    }

    /**
     * Persists the selected [languageTag] as the preferred language for AI-generated content.
     * Passing `null` reverts to the device default language.
     */
    fun onAiLanguageChange(languageTag: String?) {
        appPreferences.setAiLanguage(languageTag)
        _uiState.update { it.copy(aiLanguage = languageTag) }
    }
}

data class SettingsUiState(
    /** The IANA timezone ID selected as the app-wide default, or `null` for device default. */
    val defaultTimezone: String? = null,
    /** The BCP-47 language tag selected for AI-generated content, or `null` for device default. */
    val aiLanguage: String? = null,
)
