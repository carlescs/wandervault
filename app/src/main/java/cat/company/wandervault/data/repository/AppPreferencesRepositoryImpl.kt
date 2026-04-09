package cat.company.wandervault.data.repository

import android.content.Context
import cat.company.wandervault.domain.repository.AppPreferencesRepository

private const val PREFS_NAME = "wandervault_prefs"
private const val KEY_DEFAULT_TIMEZONE = "default_timezone"
private const val KEY_AI_LANGUAGE = "ai_language"
private const val KEY_AI_ENABLED = "ai_enabled"
private const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"

/**
 * SharedPreferences-backed implementation of [AppPreferencesRepository].
 *
 * Stores user preferences in a private SharedPreferences file so they persist across
 * app restarts.  No reactive stream is exposed because preference changes are infrequent
 * and consumers re-read on resume.
 */
class AppPreferencesRepositoryImpl(context: Context) : AppPreferencesRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun getDefaultTimezone(): String? = prefs.getString(KEY_DEFAULT_TIMEZONE, null)

    override fun setDefaultTimezone(zoneId: String?) {
        prefs.edit().apply {
            if (zoneId == null) {
                remove(KEY_DEFAULT_TIMEZONE)
            } else {
                putString(KEY_DEFAULT_TIMEZONE, zoneId)
            }
            apply()
        }
    }

    override fun getAiLanguage(): String? = prefs.getString(KEY_AI_LANGUAGE, null)

    override fun setAiLanguage(languageTag: String?) {
        prefs.edit().apply {
            if (languageTag == null) {
                remove(KEY_AI_LANGUAGE)
            } else {
                putString(KEY_AI_LANGUAGE, languageTag)
            }
            apply()
        }
    }

    override fun getAiEnabled(): Boolean = prefs.getBoolean(KEY_AI_ENABLED, true)

    override fun setAiEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AI_ENABLED, enabled).apply()
    }

    override fun getNotificationsEnabled(): Boolean =
        prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)

    override fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }
}
