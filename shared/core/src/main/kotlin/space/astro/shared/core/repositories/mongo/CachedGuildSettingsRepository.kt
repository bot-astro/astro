package space.astro.shared.core.repositories.mongo

import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity

/**
 * Fragment of [GuildSettingsRepository] that reads through a Redis cache, implemented by [CachedGuildSettingsRepositoryImpl]
 */
interface CachedGuildSettingsRepository {
    fun findByGuildID(guildID: String): GuildSettingsEntity?

    fun <S : GuildSettingsEntity> save(entity: S): S
}
