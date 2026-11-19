package space.astro.api.central.services.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import space.astro.api.central.properties.AiProperties
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.properties.PremiumProperties
import java.time.YearMonth

class AiLimitsServiceTest {
    private val limitsService =
        AiLimitsService(
            StringRedisTemplate(),
            PremiumManager(PremiumProperties("", "", "")),
            AiProperties(
                openrouterApiKey = "",
                openrouterBaseUrl = "",
                model = "",
                internalToken = "",
                maxSteps = 8,
                monthlyMessagesFree = 100,
                monthlyMessagesPremium = 1000,
                monthlyTokensFree = 50_000,
                monthlyTokensPremium = 500_000,
            ),
        )

    @Test
    fun `free and premium guilds get their limits`() {
        val free = limitsService.usageOf(10, 100, premium = false, month = YearMonth.of(2026, 11))
        val premium = limitsService.usageOf(10, 100, premium = true, month = YearMonth.of(2026, 11))

        assertEquals(100, free.messagesLimit)
        assertEquals(50_000, free.tokensLimit)
        assertEquals(1000, premium.messagesLimit)
        assertEquals(500_000, premium.tokensLimit)
    }

    @Test
    fun `the limit is reached by messages or by tokens`() {
        val month = YearMonth.of(2026, 11)

        assertFalse(limitsService.usageOf(99, 49_999, premium = false, month = month).limitReached)
        assertTrue(limitsService.usageOf(100, 0, premium = false, month = month).limitReached)
        assertTrue(limitsService.usageOf(0, 50_000, premium = false, month = month).limitReached)
        assertFalse(limitsService.usageOf(100, 50_000, premium = true, month = month).limitReached)
    }

    @Test
    fun `the counters reset at the start of the next month`() {
        assertEquals("2027-01-01T00:00:00Z", limitsService.usageOf(0, 0, premium = false, month = YearMonth.of(2026, 12)).resetsAt)
    }
}
