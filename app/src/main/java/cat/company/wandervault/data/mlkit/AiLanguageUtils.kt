package cat.company.wandervault.data.mlkit

import cat.company.wandervault.domain.repository.AppPreferencesRepository
import java.util.Locale

/**
 * Extension function that returns the English display name of the AI output language configured
 * by the user, falling back to the device default locale when no explicit preference has been set.
 *
 * The returned name is title-cased (e.g. "French", "Spanish") and suitable for inclusion
 * in a Gemini Nano prompt instruction such as "Respond in French.".
 */
internal fun AppPreferencesRepository.resolvedAiLanguageName(): String {
    val tag = getAiLanguage() ?: Locale.getDefault().toLanguageTag()
    return Locale.forLanguageTag(tag).getDisplayLanguage(Locale.ENGLISH)
        .replaceFirstChar { it.titlecase(Locale.ENGLISH) }
}
