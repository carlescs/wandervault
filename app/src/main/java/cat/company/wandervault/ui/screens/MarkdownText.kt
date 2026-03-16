package cat.company.wandervault.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cat.company.wandervault.ui.theme.WanderVaultTheme

/**
 * Renders a [markdown]-formatted string using Jetpack Compose primitives.
 *
 * Supported syntax:
 * - `# H1`, `## H2`, `### H3` headings
 * - `**bold**` and `__bold__` inline bold
 * - `*italic*` and `_italic_` inline italic
 * - `` `code` `` inline monospace code
 * - `- item` and `* item` unordered list items
 * - `1. item` ordered list items
 * - Blank lines between paragraphs
 */
@Composable
internal fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    Column(modifier = modifier) {
        blocks.forEachIndexed { index, block ->
            if (index > 0 && block is MarkdownBlock.Paragraph && blocks[index - 1] is MarkdownBlock.Paragraph) {
                Spacer(modifier = Modifier.height(4.dp))
            }
            when (block) {
                is MarkdownBlock.Heading -> {
                    val headingStyle = when (block.level) {
                        1 -> MaterialTheme.typography.titleLarge
                        2 -> MaterialTheme.typography.titleMedium
                        else -> MaterialTheme.typography.titleSmall
                    }
                    Text(
                        text = parseInlineMarkdown(block.text),
                        style = headingStyle,
                        modifier = if (index > 0) Modifier.padding(top = 8.dp) else Modifier,
                    )
                }

                is MarkdownBlock.UnorderedListItem -> {
                    Row {
                        Text(text = "• ", style = style)
                        Text(text = parseInlineMarkdown(block.text), style = style)
                    }
                }

                is MarkdownBlock.OrderedListItem -> {
                    Row {
                        Text(text = "${block.number}. ", style = style)
                        Text(text = parseInlineMarkdown(block.text), style = style)
                    }
                }

                is MarkdownBlock.Paragraph -> {
                    Text(text = parseInlineMarkdown(block.text), style = style)
                }
            }
        }
    }
}

// ── Internal model ─────────────────────────────────────────────────────────────

private sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class UnorderedListItem(val text: String) : MarkdownBlock
    data class OrderedListItem(val number: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
}

// ── Block-level parser ─────────────────────────────────────────────────────────

private val headingRegex = Regex("^(#{1,3})\\s+(.+)$")
private val unorderedListRegex = Regex("^[*\\-]\\s+(.+)$")
private val orderedListRegex = Regex("^(\\d+)\\.\\s+(.+)$")

private fun parseMarkdownBlocks(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val paragraphLines = mutableListOf<String>()

    fun flushParagraph() {
        if (paragraphLines.isNotEmpty()) {
            blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString(" ")))
            paragraphLines.clear()
        }
    }

    for (line in text.lines()) {
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> flushParagraph()

            headingRegex.matches(trimmed) -> {
                flushParagraph()
                val match = headingRegex.matchEntire(trimmed)!!
                blocks.add(MarkdownBlock.Heading(match.groupValues[1].length, match.groupValues[2]))
            }

            unorderedListRegex.matches(trimmed) -> {
                flushParagraph()
                val match = unorderedListRegex.matchEntire(trimmed)!!
                blocks.add(MarkdownBlock.UnorderedListItem(match.groupValues[1]))
            }

            orderedListRegex.matches(trimmed) -> {
                flushParagraph()
                val match = orderedListRegex.matchEntire(trimmed)!!
                blocks.add(MarkdownBlock.OrderedListItem(match.groupValues[1].toInt(), match.groupValues[2]))
            }

            else -> paragraphLines.add(trimmed)
        }
    }
    flushParagraph()
    return blocks
}

// ── Inline markup parser ───────────────────────────────────────────────────────

private val inlineRegex = Regex("`([^`]+)`|\\*\\*(.+?)\\*\\*|__(.+?)__|\\*(.+?)\\*|_(.+?)_")

/**
 * Converts inline markdown spans within a single line into an [AnnotatedString] that
 * applies [FontWeight.Bold], [FontStyle.Italic], and [FontFamily.Monospace] styles.
 *
 * Supported patterns (in evaluation order):
 * 1. `` `code` `` → monospace
 * 2. `**text**` or `__text__` → bold
 * 3. `*text*` or `_text_` → italic
 */
internal fun parseInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    for (match in inlineRegex.findAll(text)) {
        if (match.range.first > cursor) {
            append(text.substring(cursor, match.range.first))
        }
        when {
            match.groupValues[1].isNotEmpty() -> {
                // `code`
                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace))
                append(match.groupValues[1])
                pop()
            }
            match.groupValues[2].isNotEmpty() -> {
                // **bold**
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(match.groupValues[2])
                pop()
            }
            match.groupValues[3].isNotEmpty() -> {
                // __bold__
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(match.groupValues[3])
                pop()
            }
            match.groupValues[4].isNotEmpty() -> {
                // *italic*
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(match.groupValues[4])
                pop()
            }
            match.groupValues[5].isNotEmpty() -> {
                // _italic_
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(match.groupValues[5])
                pop()
            }
        }
        cursor = match.range.last + 1
    }
    if (cursor < text.length) {
        append(text.substring(cursor))
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun MarkdownTextPreview() {
    WanderVaultTheme {
        MarkdownText(
            markdown = """
                # Flight Summary
                
                ## Outbound leg
                
                Your flight departs from **Barcelona (BCN)** on _March 22_ at 08:45.
                
                Included items:
                - Carry-on bag
                - 1 checked bag (23 kg)
                
                Seat: `14A` (window)
                
                1. Check in online 24 h before departure
                2. Arrive at the airport at least 2 h early
                3. Proceed to gate **B12**
            """.trimIndent(),
        )
    }
}
