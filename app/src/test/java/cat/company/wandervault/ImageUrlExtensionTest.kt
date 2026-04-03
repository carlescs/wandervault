package cat.company.wandervault

import cat.company.wandervault.data.repository.imageUrlExtension
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [imageUrlExtension] — the helper that extracts a file extension from an image URL.
 */
class ImageUrlExtensionTest {

    @Test
    fun `simple jpg URL returns jpg`() {
        assertEquals("jpg", imageUrlExtension("https://example.com/photo/image.jpg"))
    }

    @Test
    fun `URL with query parameters strips them before extracting extension`() {
        assertEquals("jpg", imageUrlExtension("https://example.com/image.jpg?size=large&token=abc"))
    }

    @Test
    fun `png extension is preserved`() {
        assertEquals("png", imageUrlExtension("https://cdn.example.com/photos/pic.png"))
    }

    @Test
    fun `webp extension is preserved`() {
        assertEquals("webp", imageUrlExtension("https://example.com/image.webp?v=2"))
    }

    @Test
    fun `URL without extension falls back to jpg`() {
        assertEquals("jpg", imageUrlExtension("https://example.com/image"))
    }

    @Test
    fun `URL with no path segment falls back to jpg`() {
        assertEquals("jpg", imageUrlExtension("https://example.com/"))
    }

    @Test
    fun `extension longer than 4 chars is truncated to 4 chars`() {
        // e.g. "jpeg" is 4 chars → fine; "foobar" should be truncated
        val result = imageUrlExtension("https://example.com/image.foobar")
        assertEquals(4, result.length)
        assertEquals("foob", result)
    }

    @Test
    fun `extension with non-alphanumeric chars is filtered`() {
        assertEquals("jpg", imageUrlExtension("https://example.com/image.j%pg"))
    }

    @Test
    fun `empty extension with query string falls back to jpg`() {
        assertEquals("jpg", imageUrlExtension("https://example.com/image.?size=1"))
    }
}
