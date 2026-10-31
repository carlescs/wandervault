package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TranslatedText
import cat.company.wandervault.domain.repository.TranslationRepository

/**
 * Use-case that translates free text (e.g. destination notes) on-device.
 */
class TranslateTextUseCase(private val repository: TranslationRepository) {

    /**
     * Returns `true` when [text] in [sourceLanguage] can be translated into [targetLanguage]:
     * both languages are supported and they differ.
     */
    fun canTranslate(sourceLanguage: String, targetLanguage: String): Boolean =
        primaryLanguage(sourceLanguage) != primaryLanguage(targetLanguage) &&
            repository.isTranslationSupported(sourceLanguage) &&
            repository.isTranslationSupported(targetLanguage)

    /**
     * Translates [text] from [sourceLanguage] into [targetLanguage].
     *
     * @return The [TranslatedText], or `null` when the language pair is not supported.
     * @throws Exception for transient failures so callers can surface a retriable error.
     */
    suspend operator fun invoke(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): TranslatedText? {
        if (!canTranslate(sourceLanguage, targetLanguage)) return null
        val translated = repository.translate(text, sourceLanguage, targetLanguage) ?: return null
        return TranslatedText(
            text = translated,
            sourceLanguage = primaryLanguage(sourceLanguage),
            targetLanguage = primaryLanguage(targetLanguage),
        )
    }
}
