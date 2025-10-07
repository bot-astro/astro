package space.astro.shared.core.daos

import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Repository
import space.astro.shared.core.components.io.DataSerializer
import space.astro.shared.core.models.redis.RedisExpiringCacheDao
import space.astro.shared.core.models.redis.RedisKey

@Repository
class VoteDao(
    redisClusterCommands: RedisClusterCommands<String, String>,
    dataSerializer: DataSerializer
) {
    private val cacheManager = RedisExpiringCacheDao(
        keyBase = RedisKey.VOTES.key,
        redis = redisClusterCommands,
        dataSerializer = dataSerializer
    )

    fun save(userId: String, voteTimestamp: Long, expirationSeconds: Long = 43200) {
        cacheManager.cache(userId, voteTimestamp, expirationSeconds)
    }

    fun get(userId: String): Long? {
        return cacheManager.get(userId)
    }

    fun hasVoted12Hours(userId: String): Boolean {
        return get(userId)?.let { (System.currentTimeMillis() - it) > 43200000 } ?: false
    }

    fun delete(userId: String) {
        cacheManager.delete(userId)
    }
}