package space.astro.shared.core.services.dao

import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Repository
import space.astro.shared.core.components.io.DataSerializer
import space.astro.shared.core.io.caching.redis.RedisHashCacheManager
import space.astro.shared.core.io.caching.redis.RedisKey
import space.astro.shared.core.models.database.TemporaryVCData

@Repository
class TemporaryVCDao(
    redisClusterCommands: RedisClusterCommands<String, String>,
    dataSerializer: DataSerializer
) {
    private val cacheManager = RedisHashCacheManager(
        keyBase = RedisKey.TEMPORARY_VCS.key,
        redis = redisClusterCommands,
        dataSerializer = dataSerializer
    )

    fun save(guildId: String, temporaryVCData: TemporaryVCData) {
        cacheManager.cache(guildId, temporaryVCData.id, temporaryVCData)
    }

    fun getAll(guildId: String): List<TemporaryVCData> {
        return cacheManager.getAll(guildId)
    }
}