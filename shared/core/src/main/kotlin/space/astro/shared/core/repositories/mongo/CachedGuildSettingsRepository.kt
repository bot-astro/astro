package space.astro.shared.core.repositories.mongo

import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity

interface CachedGuildSettingsRepository {
    fun findByGuildID(guildID: String): GuildSettingsEntity?
}
