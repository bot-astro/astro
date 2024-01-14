package space.astro.shared.core.daos

import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Repository
import space.astro.shared.core.components.io.DataSerializer
import space.astro.shared.core.models.database.TemporaryVCData
import space.astro.shared.core.models.redis.RedisDynamicHashCacheDao
import space.astro.shared.core.models.redis.RedisKey

@Repository
class TemporaryVCDao(
    redisClusterCommands: RedisClusterCommands<String, String>,
    dataSerializer: DataSerializer
) {
    private val cacheManager = RedisDynamicHashCacheDao(
        keyBase = RedisKey.TEMPORARY_VCS.key,
        redis = redisClusterCommands,
        dataSerializer = dataSerializer
    )

    fun save(guildId: String, temporaryVCData: TemporaryVCData) {
        cacheManager.cache(guildId, temporaryVCData.id, temporaryVCData)
    }

    fun saveAll(guildId: String, temporaryVCData: List<TemporaryVCData>) {
        cacheManager.cacheAll(guildId, temporaryVCData.associateBy { it.id })
    }

    fun get(guildId: String, temporaryVCId: String): TemporaryVCData? {
        return cacheManager.get(guildId, temporaryVCId)
    }

    fun getAll(guildId: String): List<TemporaryVCData> {
        return cacheManager.getAll(guildId)
    }

    fun delete(guildId: String, vcId: String) {
        cacheManager.delete(guildId, vcId)
    }

    fun deleteAll(guildId: String) {
        cacheManager.deleteAll(guildId)
    }
}