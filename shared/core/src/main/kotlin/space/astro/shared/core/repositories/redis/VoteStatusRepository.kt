package space.astro.shared.core.repositories.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import space.astro.shared.core.utils.redis.RedisKey
import java.time.Duration
import java.time.Instant

/**
 * Active votes of the users, written by the Vote Tracker forwarding webhook and read by the vote guard.
 *
 * The key lives until the vote expires, so no key means no active vote.
 */
@Repository
class VoteStatusRepository(
    private val redis: StringRedisTemplate,
) {
    fun hasActiveVote(userId: String): Boolean = redis.hasKey(RedisKey.VOTE_STATUS(userId))

    fun save(
        userId: String,
        expiresAt: Instant,
    ) {
        val ttl = Duration.between(Instant.now(), expiresAt)
        if (ttl.isNegative || ttl.isZero) {
            return
        }

        redis.opsForValue().set(RedisKey.VOTE_STATUS(userId), "1", ttl)
    }
}
