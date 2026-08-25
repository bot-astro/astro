package space.astro.api.central.services.dashboard

import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Service
import space.astro.api.central.models.dashboard.DashboardGuildDto
import space.astro.shared.core.components.io.DataSerializer
import space.astro.shared.core.models.redis.RedisDynamicHashCacheDao
import space.astro.shared.core.models.redis.RedisKey

/**
 * Service to persist the guilds fetched for authenticated users
 */
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

    /**
     * @param userID
     *
     * @return a list of all the persisted guilds of the given user
     */
    suspend fun getUserGuilds(userID: String): List<DashboardGuildDto> {
        return cacheManager.getAll(userID)
    }

    /**
     * @param userID
     * @param guildID
     *
     * @return the persisted guild or null if missing
     */
    suspend fun getUserGuild(userID: String, guildID: String): DashboardGuildDto? {
        return cacheManager.get(userID, guildID)
    }

    /**
     * @param userID
     * @param guilds the guilds to persist
     */
    suspend fun updateUserGuilds(userID: String, guilds: List<DashboardGuildDto>) {
        cacheManager.cacheAll(userID, guilds.associateBy { it.id })
    }

    /**
     * @param userID
     */
    suspend fun deleteUserGuilds(userID: String) {
        cacheManager.deleteAll(userID)
    }
}