package cat.company.wandervault.domain.model

/**
 * The result of translating a piece of text on-device.
 *
 * @param text The translated text.
 * @param sourceLanguage BCP-47 tag of the language the original text was written in (e.g. "es").
 * @param targetLanguage BCP-47 tag of the language [text] is written in (e.g. "en").
 */
data class TranslatedText(
    val text: String,
    val sourceLanguage: String,
    val targetLanguage: String,
)
