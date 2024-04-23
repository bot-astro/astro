package space.astro.api.central.services.dashboard

import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Service
import space.astro.api.central.models.dashboard.DashboardGuildDto
import space.astro.shared.core.components.io.DataSerializer
import space.astro.shared.core.models.redis.RedisDynamicHashCacheDao
import space.astro.shared.core.models.redis.RedisKey

@Service
class DashboardGuildsPersistenceService(
    redis: RedisClusterCommands<String, String>,
    dataSerializer: DataSerializer
) {
    private val cacheManager = RedisDynamicHashCacheDao(
        keyBase = RedisKey.DASHBOARD_GUILDS.key,
        redis = redis,
        dataSerializer = dataSerializer
    )

    suspend fun getUserGuilds(userID: String): List<DashboardGuildDto> {
        return cacheManager.getAll(userID)
    }

    suspend fun getUserGuild(userID: String, guildID: String): DashboardGuildDto? {
        return cacheManager.get(userID, guildID)
    }

    suspend fun updateUserGuilds(userID: String, guilds: List<DashboardGuildDto>) {
        cacheManager.cacheAll(userID, guilds.associateBy { it.id })
    }

    suspend fun deleteUserGuilds(userID: String) {
        cacheManager.deleteAll(userID)
    }
}