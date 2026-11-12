package space.astro.shared.core.repositories.redis

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Repository
import space.astro.shared.core.models.database.TemporaryVCEntity
import space.astro.shared.core.utils.redis.RedisDynamicHashCacheRepository
import space.astro.shared.core.utils.redis.RedisKey
import tools.jackson.databind.json.JsonMapper

@Repository
class TemporaryVCRepository(
    private val redis: StringRedisTemplate,
    private val jsonMapper: JsonMapper,
) {
    private val cacheManager =
        RedisDynamicHashCacheRepository(
            keyBase = RedisKey.TEMPORARY_VCS(),
            redis = redis,
            jsonMapper = jsonMapper,
        )

    fun getOfGuild(guildId: String): List<TemporaryVCEntity> = cacheManager.getAll(guildId)

    fun get(
        guildId: String,
        vcId: String,
    ): TemporaryVCEntity? = cacheManager.get(guildId, vcId)

    fun save(
        guildId: String,
        vcEntity: TemporaryVCEntity,
    ) {
        cacheManager.cache(guildId, vcEntity.id, vcEntity)
    }

    fun saveAll(
        guildId: String,
        vcEntities: List<TemporaryVCEntity>,
    ) {
        cacheManager.cacheAll(guildId, vcEntities.associateBy { it.id })
    }

    fun delete(
        guildId: String,
        vcId: String,
    ) {
        cacheManager.delete(guildId, vcId)
    }

    fun deleteAll(guildId: String) {
        cacheManager.deleteAll(guildId)
    }
}
