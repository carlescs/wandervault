package cat.company.wandervault.domain.usecase

import cat.company.wandervault.domain.repository.TextRecognitionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [ScanTravelDocumentUseCase].
 */
class ScanTravelDocumentUseCaseTest {

    private class FakeTextRecognitionRepository(private val text: String?) : TextRecognitionRepository {
        var lastUri: String? = null
        override suspend fun recognizeText(imageUri: String): String? {
            lastUri = imageUri
            return text
        }
    }

    @Test
    fun `returns null when no text is recognised`() = runTest {
        assertNull(ScanTravelDocumentUseCase(FakeTextRecognitionRepository(null))("content://img"))
        assertNull(ScanTravelDocumentUseCase(FakeTextRecognitionRepository("   "))("content://img"))
    }

    @Test
    fun `parses recognised text`() = runTest {
        val repo = FakeTextRecognitionRepository("FLIGHT AF1234\nPNR: ZX9Y8W")
        val info = ScanTravelDocumentUseCase(repo)("content://img")

        assertEquals("content://img", repo.lastUri)
        assertEquals("AF1234", info?.flightNumber)
        assertEquals("ZX9Y8W", info?.bookingReference)
    }
}
