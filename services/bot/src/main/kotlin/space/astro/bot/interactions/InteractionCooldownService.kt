package space.astro.bot.interactions

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import space.astro.shared.core.utils.redis.RedisKey
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.toJavaDuration

@Component
class InteractionCooldownService(
    private val redis: StringRedisTemplate,
) {
    /**
     * Starts the cooldown if the user is not on cooldown for [key]
     *
     * @return the remaining cooldown, [Duration.ZERO] if the user was not on cooldown
     */
    fun acquire(
        userId: String,
        key: String,
        cooldown: Duration,
    ): Duration {
        if (cooldown <= Duration.ZERO) {
            return Duration.ZERO
        }

        val redisKey = RedisKey.INTERACTION_COOLDOWN(userId, key)

        if (redis.opsForValue().setIfAbsent(redisKey, "1", cooldown.toJavaDuration()) == true) {
            return Duration.ZERO
        }

        return redis.getExpire(redisKey, TimeUnit.MILLISECONDS).coerceAtLeast(0).milliseconds
    }
}
