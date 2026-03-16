package cat.company.wandervault.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [parseInlineMarkdown].
 *
 * These tests verify that markdown delimiters are stripped and the resulting plain text
 * is correct. Span-style assertions are intentionally omitted to keep tests independent
 * of the Compose runtime.
 */
class MarkdownTextTest {

    // ── parseInlineMarkdown – text content ─────────────────────────────────────

    @Test
    fun `plain text is returned unchanged`() {
        assertEquals("Hello world", parseInlineMarkdown("Hello world").text)
    }

    @Test
    fun `bold asterisks stripped from text`() {
        assertEquals("Say hello now", parseInlineMarkdown("Say **hello** now").text)
    }

    @Test
    fun `bold underscores stripped from text`() {
        assertEquals("bold", parseInlineMarkdown("__bold__").text)
    }

    @Test
    fun `italic asterisk stripped from text`() {
        assertEquals("italic", parseInlineMarkdown("*italic*").text)
    }

    @Test
    fun `italic underscore stripped from text`() {
        assertEquals("italic", parseInlineMarkdown("_italic_").text)
    }

    @Test
    fun `backtick code stripped from text`() {
        assertEquals("code", parseInlineMarkdown("`code`").text)
    }

    @Test
    fun `multiple spans in one line – text content correct`() {
        assertEquals("bold and italic", parseInlineMarkdown("**bold** and *italic*").text)
    }

    @Test
    fun `empty string returns empty text`() {
        assertEquals("", parseInlineMarkdown("").text)
    }

    @Test
    fun `text with no markdown delimiters unchanged`() {
        assertEquals("No formatting here!", parseInlineMarkdown("No formatting here!").text)
    }
}
