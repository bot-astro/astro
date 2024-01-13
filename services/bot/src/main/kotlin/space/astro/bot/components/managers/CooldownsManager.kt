package space.astro.bot.components.managers

import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Component
import space.astro.bot.config.ApplicationFeaturesConfig
import space.astro.bot.interactions.InteractionAction
import space.astro.shared.core.models.redis.RedisKey

@Component
class CooldownsManager(
    private val applicationFeaturesConfig: ApplicationFeaturesConfig,
    private val redis: RedisClusterCommands<String, String>
) {
    // TODO: Global cooldown for users?
    fun getUserGeneratorsCooldown(userId: String): Long {
        val now = System.currentTimeMillis()

        val timestamp = redis.hget(RedisKey.GENERATOR_RATELIMIT_FOR_USER.key, userId)
            ?.toLongOrNull()
            ?: run {
                redis.hset(RedisKey.GENERATOR_RATELIMIT_FOR_USER.key, userId, now.toString())
                return 0
            }

        val timeDifference = now - timestamp

        return if (timeDifference < applicationFeaturesConfig.generatorCooldown) {
            timeDifference
        } else {
            redis.hset(RedisKey.GENERATOR_RATELIMIT_FOR_USER.key, userId, now.toString())
            0
        }
    }

    fun getUserActionCooldown(userId: String, action: InteractionAction): Long {
        val now = System.currentTimeMillis()
        val field = "${userId}_${action.name}"

        val timestamp = redis.hget(RedisKey.COMMAND_RATELIMIT_FOR_USER.key, field)
            ?.toLongOrNull()
            ?: run {
                redis.hset(RedisKey.COMMAND_RATELIMIT_FOR_USER.key, field, now.toString())
                return 0
            }

        val timeDifference = now - timestamp
        return if (timeDifference < action.cooldown) {
            timeDifference
        } else {
            redis.hset(RedisKey.COMMAND_RATELIMIT_FOR_USER.key, field, now.toString())
            0
        }
    }

    fun markUserGeneratorsCooldown(userId: String) {
        redis.hset(
            RedisKey.GENERATOR_RATELIMIT_FOR_USER.key,
            userId,
            System.currentTimeMillis().toString()
        )
    }
}