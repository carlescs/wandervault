package cat.company.wandervault.data.repository

import android.content.Context
import cat.company.wandervault.domain.repository.AppPreferencesRepository

private const val PREFS_NAME = "wandervault_prefs"
private const val KEY_DEFAULT_TIMEZONE = "default_timezone"

/**
 * SharedPreferences-backed implementation of [AppPreferencesRepository].
 *
 * Stores user preferences in a private SharedPreferences file so they persist across
 * app restarts.  No reactive stream is exposed because timezone changes are infrequent
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
}
