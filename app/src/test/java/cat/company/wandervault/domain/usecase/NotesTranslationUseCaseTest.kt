package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.model.TranslatedText
import cat.company.wandervault.domain.repository.AppPreferencesRepository
import cat.company.wandervault.domain.repository.TranslationRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

/**
 * Unit tests for the notes language detection and translation use-cases.
 */
class NotesTranslationUseCaseTest {

    private class FakeTranslationRepository(
        private val detected: String? = "es",
        private val supported: Set<String> = setOf("en", "es", "fr"),
    ) : TranslationRepository {
        val detectCalls = mutableListOf<String>()
        val translateCalls = mutableListOf<Triple<String, String, String>>()

        override suspend fun detectLanguage(text: String): String? {
            detectCalls += text
            return detected
        }

        override fun isTranslationSupported(languageTag: String): Boolean =
            primaryLanguage(languageTag) in supported

        override suspend fun translate(
            text: String,
            sourceLanguage: String,
            targetLanguage: String,
        ): String {
            translateCalls += Triple(text, sourceLanguage, targetLanguage)
            return "[$targetLanguage] $text"
        }
    }

    private class FakePreferences(private val aiLanguage: String?) : AppPreferencesRepository {
        override fun getDefaultTimezone(): String? = null
        override fun setDefaultTimezone(zoneId: String?) = Unit
        override fun getAiLanguage(): String? = aiLanguage
        override fun setAiLanguage(languageTag: String?) = Unit
        override fun getAiEnabled(): Boolean = true
        override fun setAiEnabled(enabled: Boolean) = Unit
        override fun getNotificationsEnabled(): Boolean = true
        override fun setNotificationsEnabled(enabled: Boolean) = Unit
    }

    // ── DetectLanguageUseCase ─────────────────────────────────────────────────

    @Test
    fun `detect returns null for blank or very short text without calling the repository`() = runTest {
        val repo = FakeTranslationRepository()
        val useCase = DetectLanguageUseCase(repo)

        assertNull(useCase("   "))
        assertNull(useCase("ok"))
        assertTrue(repo.detectCalls.isEmpty())
    }

    @Test
    fun `detect trims text and returns the repository result`() = runTest {
        val repo = FakeTranslationRepository(detected = "fr")
        val useCase = DetectLanguageUseCase(repo)

        assertEquals("fr", useCase("  Bonjour tout le monde  "))
        assertEquals(listOf("Bonjour tout le monde"), repo.detectCalls)
    }

    // ── TranslateTextUseCase ──────────────────────────────────────────────────

    @Test
    fun `canTranslate is false for the same primary language`() {
        val useCase = TranslateTextUseCase(FakeTranslationRepository())
        assertFalse(useCase.canTranslate("en", "en-US"))
    }

    @Test
    fun `canTranslate is false when either language is unsupported`() {
        val useCase = TranslateTextUseCase(FakeTranslationRepository())
        assertFalse(useCase.canTranslate("xx", "en"))
        assertFalse(useCase.canTranslate("es", "xx"))
    }

    @Test
    fun `canTranslate is true for distinct supported languages`() {
        val useCase = TranslateTextUseCase(FakeTranslationRepository())
        assertTrue(useCase.canTranslate("es", "en-GB"))
    }

    @Test
    fun `translate returns translated text with normalised language tags`() = runTest {
        val repo = FakeTranslationRepository()
        val useCase = TranslateTextUseCase(repo)

        val result = useCase("Hola", "es", "en-US")

        assertEquals(TranslatedText("[en-US] Hola", "es", "en"), result)
        assertEquals(listOf(Triple("Hola", "es", "en-US")), repo.translateCalls)
    }

    @Test
    fun `translate returns null for unsupported pair without calling the repository`() = runTest {
        val repo = FakeTranslationRepository()
        val useCase = TranslateTextUseCase(repo)

        assertNull(useCase("Hola", "es", "xx"))
        assertTrue(repo.translateCalls.isEmpty())
    }

    // ── GetTranslationTargetLanguageUseCase ───────────────────────────────────

    @Test
    fun `target language uses the AI language preference when set`() {
        val useCase = GetTranslationTargetLanguageUseCase(FakePreferences("fr-CA")) { Locale.US }
        assertEquals("fr", useCase())
    }

    @Test
    fun `target language falls back to the device locale`() {
        val useCase = GetTranslationTargetLanguageUseCase(FakePreferences(null)) { Locale.GERMANY }
        assertEquals("de", useCase())
    }

    @Test
    fun `primaryLanguage strips region and lower-cases`() {
        assertEquals("pt", primaryLanguage("PT-br"))
        assertEquals("zh", primaryLanguage("zh_Hant"))
        assertEquals("es", primaryLanguage("es"))
    }
}
