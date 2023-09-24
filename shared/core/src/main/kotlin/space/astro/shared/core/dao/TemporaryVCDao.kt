package space.astro.shared.core.dao

import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Component
import space.astro.shared.core.components.io.DataSerializer
import space.astro.shared.core.io.caching.redis.RedisHashCacheManager
import space.astro.shared.core.io.caching.redis.RedisKey
import space.astro.shared.core.models.database.TemporaryVCDto

@Component
class TemporaryVCDao(
    redisClusterCommands: RedisClusterCommands<String, String>,
    dataSerializer: DataSerializer
) {
    private val cacheManager = RedisHashCacheManager(
        keyBase = RedisKey.TEMPORARY_VCS.key,
        redis = redisClusterCommands,
        dataSerializer = dataSerializer
    )

    fun getAll(guildId: String): List<TemporaryVCDto> {
        return cacheManager.getAll(guildId)
    }
}