package space.astro.api.central.services.ai

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import space.astro.api.central.models.ai.AiUsageResponse
import space.astro.api.central.properties.AiProperties
import space.astro.shared.core.clients.PremiumManager
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.utils.redis.RedisKey
import java.time.Duration
import java.time.YearMonth
import java.time.ZoneOffset

/**
 * Monthly message and token budget of the assistant per guild, premium guilds get higher limits
 */
@Service
class AiLimitsService(
    private val redis: StringRedisTemplate,
    private val premiumManager: PremiumManager,
    private val aiProperties: AiProperties,
) {
    private companion object {
        const val MESSAGES_FIELD = "messages"
        const val TOKENS_FIELD = "tokens"

        // longer than any month, the key of a month is not read anymore once it ends
        val COUNTERS_TTL: Duration = Duration.ofDays(40)
    }

    fun getUsage(
        guildSettings: GuildSettingsEntity,
        month: YearMonth = YearMonth.now(ZoneOffset.UTC),
    ): AiUsageResponse {
        val counters =
            redis
                .opsForHash<String, String>()
                .entries(RedisKey.AI_LIMITS(guildSettings.guildID, month.toString()))
        val premium = premiumManager.isGuildPremium(guildSettings)

        return usageOf(
            messagesUsed = counters[MESSAGES_FIELD]?.toLong() ?: 0,
            tokensUsed = counters[TOKENS_FIELD]?.toLong() ?: 0,
            premium = premium,
            month = month,
        )
    }

    fun usageOf(
        messagesUsed: Long,
        tokensUsed: Long,
        premium: Boolean,
        month: YearMonth,
    ) = AiUsageResponse(
        messagesUsed = messagesUsed,
        messagesLimit = if (premium) aiProperties.monthlyMessagesPremium else aiProperties.monthlyMessagesFree,
        tokensUsed = tokensUsed,
        tokensLimit = if (premium) aiProperties.monthlyTokensPremium else aiProperties.monthlyTokensFree,
        premium = premium,
        resetsAt =
            month
                .plusMonths(1)
                .atDay(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC)
                .toString(),
    )

    fun record(
        guildId: String,
        tokens: Long,
        month: YearMonth = YearMonth.now(ZoneOffset.UTC),
    ) {
        val key = RedisKey.AI_LIMITS(guildId, month.toString())
        val hashOps = redis.opsForHash<String, String>()
        hashOps.increment(key, MESSAGES_FIELD, 1)
        hashOps.increment(key, TOKENS_FIELD, tokens)
        redis.expire(key, COUNTERS_TTL)
    }
}
