package cat.company.wandervault.domain.repository

/**
 * Repository that detects the language of free text and translates it on-device.
 *
 * Language tags are BCP-47 strings such as "en", "es" or "zh". Implementations only look at the
 * primary language subtag when deciding whether a language pair is supported.
 */
interface TranslationRepository {

    /**
     * Detects the language [text] is written in.
     *
     * @return The BCP-47 language tag of the most likely language, or `null` when [text] is blank
     *   or the language cannot be determined with enough confidence.
     * @throws Exception when language identification fails unexpectedly.
     */
    suspend fun detectLanguage(text: String): String?

    /**
     * Returns `true` when on-device translation supports [languageTag] (as either source or
     * target language).
     */
    fun isTranslationSupported(languageTag: String): Boolean

    /**
     * Translates [text] from [sourceLanguage] to [targetLanguage].
     *
     * The required translation models may be downloaded the first time a language pair is used.
     *
     * @return The translated text, or `null` when the language pair is not supported.
     * @throws Exception for transient failures (e.g. model download error, translation error).
     */
    suspend fun translate(text: String, sourceLanguage: String, targetLanguage: String): String?
}
