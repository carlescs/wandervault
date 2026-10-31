package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.AppPreferencesRepository
import java.util.Locale

/**
 * Use-case that resolves the language notes should be translated into.
 *
 * Uses the user's configured AI output language when set, otherwise falls back to the device
 * default locale. The result is a primary-language BCP-47 tag (e.g. "en", "fr").
 *
 * @param appPreferences Source of the user's AI output language preference.
 * @param defaultLocale Supplies the device default locale; overridable for tests.
 */
class GetTranslationTargetLanguageUseCase(
    private val appPreferences: AppPreferencesRepository,
    private val defaultLocale: () -> Locale = { Locale.getDefault() },
) {
    operator fun invoke(): String {
        val tag = appPreferences.getAiLanguage()?.takeIf { it.isNotBlank() }
            ?: defaultLocale().toLanguageTag()
        return primaryLanguage(tag)
    }
}

/**
 * Returns the lower-cased primary language subtag of a BCP-47 [tag] (e.g. "en-US" → "en").
 */
fun primaryLanguage(tag: String): String = tag.substringBefore('-').substringBefore('_').lowercase(Locale.ROOT)
