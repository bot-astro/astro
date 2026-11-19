package space.astro.bot.ui

import space.astro.bot.components.ai.AiDiffEntry
import space.astro.bot.components.ai.AiProposal
import space.astro.shared.core.lang.L
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiMessagesTest {
    @Test
    fun `sanitize breaks every mention`() {
        val sanitized = AiMessages.sanitize("@everyone <@123> <@&456> @here")

        assertFalse(sanitized.contains("@everyone"))
        assertFalse(sanitized.contains("<@123>"))
        assertFalse(sanitized.contains("<@&456>"))
        assertFalse(sanitized.contains("@here"))
    }

    @Test
    fun `fit keeps short text and truncates long text to the max`() {
        assertEquals("short", AiMessages.fit("short", 10))

        val fitted = AiMessages.fit("a".repeat(50), 10)
        assertEquals(10, fitted.length)
        assertTrue(fitted.endsWith("…"))
    }

    @Test
    fun `fit does not cut an emoji in half`() {
        val fitted = AiMessages.fit("ab" + "😀".repeat(5), 4)

        assertTrue(fitted.none { it.isHighSurrogate() && !fitted[fitted.indexOf(it) + 1].isLowSurrogate() })
        assertTrue(fitted.length <= 4)
    }

    @Test
    fun `proposal text caps the diff lines and the total length`() {
        val proposal =
            AiProposal(
                id = "p1",
                summary = "Create generator",
                diff = (1..20).map { AiDiffEntry("field$it", null, "value$it") },
            )

        val text = AiMessages.proposalText(L.default, proposal)

        assertTrue(text.startsWith("### Create generator"))
        assertTrue(text.contains("field8"))
        assertFalse(text.contains("field9"))
        assertTrue(text.contains("12 more changes"))
        assertTrue(text.length <= 600)
    }

    @Test
    fun `proposal text shows an empty value for missing before`() {
        val text = AiMessages.proposalText(L.default, AiProposal("p1", summary = "s", diff = listOf(AiDiffEntry("name", null, "Gaming"))))

        assertTrue(text.contains("**name**: *empty* → Gaming"))
    }
}
