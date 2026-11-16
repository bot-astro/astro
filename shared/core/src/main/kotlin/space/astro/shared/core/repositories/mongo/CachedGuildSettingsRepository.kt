package space.astro.shared.core.repositories.mongo

import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity

/**
 * Fragment of [GuildSettingsRepository] that reads through a Redis cache.
 * [CachedGuildSettingsRepositoryImpl] also invalidates the cache on saves.
 */
interface CachedGuildSettingsRepository {
    fun findByGuildID(guildID: String): GuildSettingsEntity?
}
