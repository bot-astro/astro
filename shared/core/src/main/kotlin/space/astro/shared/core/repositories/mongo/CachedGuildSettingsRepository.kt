package space.astro.shared.core.repositories.mongo

import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity

/**
 * Fragment of [GuildSettingsRepository] that reads through a Redis cache, implemented by [CachedGuildSettingsRepositoryImpl], the cache is invalidated by [space.astro.shared.core.repositories.redis.GuildSettingsCacheInvalidator]
 */
interface CachedGuildSettingsRepository {
    fun findByGuildID(guildID: String): GuildSettingsEntity?
}
