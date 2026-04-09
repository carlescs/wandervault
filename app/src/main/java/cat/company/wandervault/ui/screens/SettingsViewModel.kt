package cat.company.wandervault.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cat.company.wandervault.data.notification.TripNotificationWorker
import cat.company.wandervault.domain.repository.AppPreferencesRepository
import cat.company.wandervault.domain.usecase.GenerateTripDescriptionUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the current app-wide settings state and handles user interactions.
 *
 * @param appPreferences Repository for reading and persisting app-wide preferences.
 * @param generateTripDescriptionUseCase Use-case used to check whether Gemini Nano is supported
 *   by this device (hardware check, independent of user preference). The check is performed
 *   asynchronously on init and populates [SettingsUiState.aiDeviceSupport].
 */
class SettingsViewModel(
    private val appPreferences: AppPreferencesRepository,
    private val generateTripDescriptionUseCase: GenerateTripDescriptionUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            defaultTimezone = appPreferences.getDefaultTimezone(),
            aiLanguage = appPreferences.getAiLanguage(),
            aiEnabled = appPreferences.getAiEnabled(),
            notificationsEnabled = appPreferences.getNotificationsEnabled(),
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Check whether the device hardware supports Gemini Nano, independently of the user's
        // AI preference, so the Settings UI can show the correct state.
        viewModelScope.launch {
            val support = try {
                if (generateTripDescriptionUseCase.isDeviceSupported()) {
                    AiDeviceSupport.SUPPORTED
                } else {
                    AiDeviceSupport.UNSUPPORTED
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "AI device support check failed; assuming unsupported", e)
                AiDeviceSupport.UNSUPPORTED
            }
            _uiState.update { it.copy(aiDeviceSupport = support) }
        }
    }

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

    /**
     * Enables or disables all AI features app-wide.
     */
    fun onAiEnabledChange(enabled: Boolean) {
        appPreferences.setAiEnabled(enabled)
        _uiState.update { it.copy(aiEnabled = enabled) }
    }

    /**
     * Toggles trip approach notifications.  When [enabled] is `true` the periodic worker is
     * scheduled; when `false` the worker and any visible notifications are cancelled.
     *
     * On Android 13+ the caller must obtain [android.Manifest.permission.POST_NOTIFICATIONS]
     * before calling this with [enabled] = `true`.
     */
    fun onNotificationsEnabledChange(context: Context, enabled: Boolean) {
        viewModelScope.launch {
            appPreferences.setNotificationsEnabled(enabled)
            if (enabled) {
                TripNotificationWorker.schedule(context)
            } else {
                TripNotificationWorker.cancel(context)
            }
            _uiState.update { it.copy(notificationsEnabled = enabled) }
        }
    }

    /**
     * Called when the system permission result is known for [android.Manifest.permission.POST_NOTIFICATIONS].
     * Syncs the stored preference with the actual runtime permission state.
     */
    fun onNotificationPermissionResult(context: Context, granted: Boolean) {
        viewModelScope.launch {
            appPreferences.setNotificationsEnabled(granted)
            if (granted) {
                TripNotificationWorker.schedule(context)
            } else {
                TripNotificationWorker.cancel(context)
            }
            _uiState.update { it.copy(notificationsEnabled = granted) }
        }
    }

    /**
     * Refreshes the UI state to reflect the current runtime notification permission.
     * Call this when the screen is shown so the toggle stays in sync if the user changes the
     * permission from the system settings.
     */
    fun refreshNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            viewModelScope.launch {
                val granted = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
                if (!granted && _uiState.value.notificationsEnabled) {
                    appPreferences.setNotificationsEnabled(false)
                    TripNotificationWorker.cancel(context)
                    _uiState.update { it.copy(notificationsEnabled = false) }
                }
            }
        }
    }

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}

data class SettingsUiState(
    /** The IANA timezone ID selected as the app-wide default, or `null` for device default. */
    val defaultTimezone: String? = null,
    /** The BCP-47 language tag selected for AI-generated content, or `null` for device default. */
    val aiLanguage: String? = null,
    /** Whether the user has enabled AI features. */
    val aiEnabled: Boolean = true,
    /**
     * Hardware support state for Gemini Nano. Starts as [AiDeviceSupport.CHECKING] while the
     * async check is in progress, then resolves to [AiDeviceSupport.SUPPORTED] or
     * [AiDeviceSupport.UNSUPPORTED].
     */
    val aiDeviceSupport: AiDeviceSupport = AiDeviceSupport.CHECKING,
    /** Whether the user has enabled trip approach notifications. */
    val notificationsEnabled: Boolean = true,
)

/** Hardware-support state for Gemini Nano on the current device. */
enum class AiDeviceSupport {
    /** The async hardware check is still in progress. */
    CHECKING,

    /** The device supports Gemini Nano (model is available or downloadable). */
    SUPPORTED,

    /** The device does not support Gemini Nano. */
    UNSUPPORTED,
}
