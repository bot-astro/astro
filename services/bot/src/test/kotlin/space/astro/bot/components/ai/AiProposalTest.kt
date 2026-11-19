package space.astro.bot.components.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AiProposalTest {
    private fun proposal(expiresAt: String?) = AiProposal(id = "p1", expiresAt = expiresAt)

    @Test
    fun `parses an iso instant`() {
        assertEquals(1_700_000_000L, proposal("2023-11-14T22:13:20Z").expiresAtEpochSeconds())
    }

    @Test
    fun `parses epoch seconds and milliseconds`() {
        assertEquals(1_700_000_000L, proposal("1700000000").expiresAtEpochSeconds())
        assertEquals(1_700_000_000L, proposal("1700000000000").expiresAtEpochSeconds())
    }

    @Test
    fun `returns null for missing or invalid values`() {
        assertNull(proposal(null).expiresAtEpochSeconds())
        assertNull(proposal(" ").expiresAtEpochSeconds())
        assertNull(proposal("tomorrow").expiresAtEpochSeconds())
    }
}
