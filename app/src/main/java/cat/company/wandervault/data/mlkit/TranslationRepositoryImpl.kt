package cat.company.wandervault.data.mlkit

import cat.company.wandervault.domain.repository.TranslationRepository
import cat.company.wandervault.domain.usecase.primaryLanguage
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentifier
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ML Kit implementation of [TranslationRepository].
 *
 * - Language detection uses the on-device ML Kit Language Identification API.
 * - Translation uses the on-device ML Kit Translation API. Translation models (~30 MB per
 *   language) are downloaded on first use of a language and cached by ML Kit afterwards, so
 *   subsequent translations work offline.
 */
class TranslationRepositoryImpl : TranslationRepository {

    private val languageIdentifier by lazy { LanguageIdentification.getClient() }

    override suspend fun detectLanguage(text: String): String? = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext null
        val tag = languageIdentifier.identifyLanguage(text).awaitResult()
        tag?.takeIf { it != LanguageIdentifier.UNDETERMINED_LANGUAGE_TAG }
    }

    override fun isTranslationSupported(languageTag: String): Boolean =
        TranslateLanguage.fromLanguageTag(primaryLanguage(languageTag)) != null

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String,
    ): String? = withContext(Dispatchers.IO) {
        val source = TranslateLanguage.fromLanguageTag(primaryLanguage(sourceLanguage))
            ?: return@withContext null
        val target = TranslateLanguage.fromLanguageTag(primaryLanguage(targetLanguage))
            ?: return@withContext null
        if (source == target) return@withContext text
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(source)
            .setTargetLanguage(target)
            .build()
        val translator = Translation.getClient(options)
        try {
            translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitResult()
            translator.translate(text).awaitResult()
        } finally {
            translator.close()
        }
    }
}
