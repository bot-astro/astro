package space.astro.shared.core.repositories.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import space.astro.shared.core.utils.redis.RedisKey
import java.time.Duration

/**
 * Caches the vote status of the users, so the Vote Tracker API is not called on every interaction.
 *
 * Active votes are cached longer, a user who just voted must not wait long to be unlocked.
 */
@Repository
class VoteStatusRepository(
    private val redis: StringRedisTemplate,
) {
    companion object {
        private val ACTIVE_TTL = Duration.ofHours(1)
        private val INACTIVE_TTL = Duration.ofMinutes(1)
    }

    fun get(userId: String): Boolean? = redis.opsForValue().get(RedisKey.VOTE_STATUS(userId))?.toBooleanStrictOrNull()

    fun save(
        userId: String,
        hasVoted: Boolean,
    ) {
        redis.opsForValue().set(RedisKey.VOTE_STATUS(userId), hasVoted.toString(), if (hasVoted) ACTIVE_TTL else INACTIVE_TTL)
    }
}
