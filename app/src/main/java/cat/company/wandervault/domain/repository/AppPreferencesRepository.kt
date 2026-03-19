package cat.company.wandervault.domain.repository

/**
 * Repository for app-wide user preferences that persist across sessions.
 */
interface AppPreferencesRepository {
    /**
     * Returns the IANA timezone ID chosen by the user as the app-wide default, or `null` if the
     * user has not set a preference (meaning the device timezone should be used).
     */
    fun getDefaultTimezone(): String?

    /**
     * Persists [zoneId] as the app-wide default timezone.  Pass `null` to revert to the device
     * default.
     */
    fun setDefaultTimezone(zoneId: String?)

    /**
     * Returns the BCP-47 language tag chosen by the user for AI-generated content, or `null` if
     * the user has not set a preference (meaning the device language should be used).
     */
    fun getAiLanguage(): String?

    /**
     * Persists [languageTag] as the preferred language for AI-generated content.
     * Pass `null` to revert to the device default language.
     */
    fun setAiLanguage(languageTag: String?)
}
