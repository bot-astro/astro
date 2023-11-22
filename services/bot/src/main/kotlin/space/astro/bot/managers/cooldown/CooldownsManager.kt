package space.astro.bot.managers.cooldown

import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Component
import space.astro.bot.config.ApplicationFeaturesConfig
import space.astro.shared.core.io.caching.redis.RedisKey

@Component
class CooldownsManager(
    val applicationFeaturesConfig: ApplicationFeaturesConfig,
    val redis: RedisClusterCommands<String, String>
) {
    // TODO: Global cooldown for users?
    fun getUserGeneratorsCooldown(userId: String): Long {
        val now = System.currentTimeMillis()

        val timestamp = redis.hget(RedisKey.GENERATOR_RATELIMIT.key, userId)
            ?.toLongOrNull()
            ?: run {
                redis.hset(RedisKey.GENERATOR_RATELIMIT.key, userId, now.toString())
                return 0
            }

        val timeDifference = now - timestamp

        return if (timeDifference < applicationFeaturesConfig.generatorCooldown) {
            timeDifference
        } else {
            redis.hset(RedisKey.GENERATOR_RATELIMIT.key, userId, now.toString())
            0
        }
    }

    fun markUserGeneratorsCooldown(userId: String) {
        redis.hset(
            RedisKey.GENERATOR_RATELIMIT.key,
            userId,
            System.currentTimeMillis().toString()
        )
    }
}