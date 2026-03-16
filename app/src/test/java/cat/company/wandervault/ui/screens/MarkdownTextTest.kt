package cat.company.wandervault.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [parseInlineMarkdown] and [parseMarkdownBlocks].
 *
 * Text-content assertions verify delimiter stripping. Span-style assertions are
 * intentionally omitted to keep tests independent of the Compose runtime.
 */
class MarkdownTextTest {

    // ── parseInlineMarkdown – happy-path delimiter stripping ───────────────────

    @Test
    fun `plain text is returned unchanged`() {
        assertEquals("Hello world", parseInlineMarkdown("Hello world").text)
    }

    @Test
    fun `bold asterisks stripped from text`() {
        assertEquals("Say hello now", parseInlineMarkdown("Say **hello** now").text)
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

    // ── parseInlineMarkdown – false-positive / regression tests ───────────────

    @Test
    fun `snake_case underscores are NOT treated as italic markers`() {
        // foo_bar_baz must remain untouched — the _ is between word chars
        assertEquals("foo_bar_baz", parseInlineMarkdown("foo_bar_baz").text)
    }

    @Test
    fun `multiple underscores in word left intact`() {
        assertEquals("get_trip_id()", parseInlineMarkdown("get_trip_id()").text)
    }

    @Test
    fun `double underscore syntax creates bold text`() {
        assertEquals("bold", parseInlineMarkdown("__bold__").text)
    }

    @Test
    fun `asterisk used as multiplication operator is not italic`() {
        // "2*x" has * preceded by a word char – must not be consumed
        assertEquals("2*x is a product", parseInlineMarkdown("2*x is a product").text)
    }

    @Test
    fun `italic asterisk surrounded by spaces still works`() {
        // A proper *italic* token surrounded by spaces must still be recognised
        assertEquals("see italic for details", parseInlineMarkdown("see *italic* for details").text)
    }

    @Test
    fun `italic underscore at start of sentence works`() {
        assertEquals("note this", parseInlineMarkdown("_note_ this").text)
    }

    // ── parseMarkdownBlocks – block-level parsing ─────────────────────────────

    @Test
    fun `h1 heading is parsed as Heading level 1`() {
        val blocks = parseMarkdownBlocks("# Hello")
        assertEquals(1, blocks.size)
        val heading = blocks[0] as MarkdownBlock.Heading
        assertEquals(1, heading.level)
        assertEquals("Hello", heading.text)
    }

    @Test
    fun `h2 heading is parsed as Heading level 2`() {
        val blocks = parseMarkdownBlocks("## Section")
        assertEquals(1, blocks.size)
        val heading = blocks[0] as MarkdownBlock.Heading
        assertEquals(2, heading.level)
        assertEquals("Section", heading.text)
    }

    @Test
    fun `h3 heading is parsed as Heading level 3`() {
        val blocks = parseMarkdownBlocks("### Sub")
        assertEquals(1, blocks.size)
        val heading = blocks[0] as MarkdownBlock.Heading
        assertEquals(3, heading.level)
        assertEquals("Sub", heading.text)
    }

    @Test
    fun `unordered dash list item is parsed correctly`() {
        val blocks = parseMarkdownBlocks("- item one")
        assertEquals(1, blocks.size)
        val item = blocks[0] as MarkdownBlock.UnorderedListItem
        assertEquals("item one", item.text)
    }

    @Test
    fun `unordered asterisk list item is parsed correctly`() {
        val blocks = parseMarkdownBlocks("* item two")
        assertEquals(1, blocks.size)
        val item = blocks[0] as MarkdownBlock.UnorderedListItem
        assertEquals("item two", item.text)
    }

    @Test
    fun `ordered list item is parsed with correct number`() {
        val blocks = parseMarkdownBlocks("3. third step")
        assertEquals(1, blocks.size)
        val item = blocks[0] as MarkdownBlock.OrderedListItem
        assertEquals(3, item.number)
        assertEquals("third step", item.text)
    }

    @Test
    fun `plain paragraph line is parsed as Paragraph`() {
        val blocks = parseMarkdownBlocks("This is a sentence.")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        assertEquals("This is a sentence.", (blocks[0] as MarkdownBlock.Paragraph).text)
    }

    @Test
    fun `consecutive lines are merged into a single paragraph`() {
        val blocks = parseMarkdownBlocks("line one\nline two\nline three")
        assertEquals(1, blocks.size)
        assertEquals("line one line two line three", (blocks[0] as MarkdownBlock.Paragraph).text)
    }

    @Test
    fun `blank line separates two paragraphs`() {
        val blocks = parseMarkdownBlocks("first\n\nsecond")
        assertEquals(2, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Paragraph)
        assertTrue(blocks[1] is MarkdownBlock.Paragraph)
    }

    @Test
    fun `heading followed by list followed by paragraph`() {
        val input = "# Title\n- item\nSome prose"
        val blocks = parseMarkdownBlocks(input)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is MarkdownBlock.Heading)
        assertTrue(blocks[1] is MarkdownBlock.UnorderedListItem)
        assertTrue(blocks[2] is MarkdownBlock.Paragraph)
    }

    @Test
    fun `empty string produces no blocks`() {
        val blocks = parseMarkdownBlocks("")
        assertTrue(blocks.isEmpty())
    }

    @Test
    fun `only blank lines produce no blocks`() {
        val blocks = parseMarkdownBlocks("\n\n\n")
        assertTrue(blocks.isEmpty())
    }

    @Test
    fun `heading text does not include hash characters`() {
        val blocks = parseMarkdownBlocks("## My Heading")
        val heading = blocks[0] as MarkdownBlock.Heading
        assertFalse(heading.text.startsWith("#"))
    }
}
