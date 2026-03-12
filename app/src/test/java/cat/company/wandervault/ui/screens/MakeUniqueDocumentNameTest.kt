package cat.company.wandervault.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for the [makeUniqueDocumentName] helper.
 */
class MakeUniqueDocumentNameTest {

    @Test
    fun `returns candidate unchanged when no conflict`() {
        val result = makeUniqueDocumentName("Paris Flight", emptySet())
        assertEquals("Paris Flight", result)
    }

    @Test
    fun `appends 2 when candidate already exists`() {
        val result = makeUniqueDocumentName("Paris Flight", setOf("Paris Flight"))
        assertEquals("Paris Flight 2", result)
    }

    @Test
    fun `increments counter until a unique name is found`() {
        val existing = setOf("Paris Flight", "Paris Flight 2", "Paris Flight 3")
        val result = makeUniqueDocumentName("Paris Flight", existing)
        assertEquals("Paris Flight 4", result)
    }

    @Test
    fun `only conflicts on the exact base candidate, not numbered variants`() {
        // "Paris Flight 2" is taken but "Paris Flight" is free — must return "Paris Flight"
        val result = makeUniqueDocumentName("Paris Flight", setOf("Paris Flight 2"))
        assertEquals("Paris Flight", result)
    }
}
