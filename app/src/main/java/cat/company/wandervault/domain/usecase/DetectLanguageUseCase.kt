package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TranslationRepository

/**
 * Use-case that detects the language of a piece of free text (e.g. destination notes).
 *
 * Returns the BCP-47 language tag of the detected language, or `null` when [text] is too short
 * or the language cannot be determined.
 */
class DetectLanguageUseCase(private val repository: TranslationRepository) {
    suspend operator fun invoke(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.length < MIN_TEXT_LENGTH) return null
        return repository.detectLanguage(trimmed)
    }

    companion object {
        /** Texts shorter than this are too ambiguous for reliable language identification. */
        const val MIN_TEXT_LENGTH = 4
    }
}
