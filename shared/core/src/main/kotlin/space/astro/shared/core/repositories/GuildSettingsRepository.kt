package space.astro.shared.core.repositories

import org.springframework.data.mongodb.repository.MongoRepository
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity


interface GuildSettingsRepository : MongoRepository<GuildSettingsEntity, String> {
    fun createNewGuildSettings(guildID: String): GuildSettingsEntity {
        val guildSettings = GuildSettingsEntity(
            guildID = guildID,
            upgradedByUserID = null,
            entitlements = mutableListOf(),
            templates = mutableListOf(),
            connections = mutableListOf(),
            interfaces = mutableListOf(),
            generators = mutableListOf(),
            allowMissingAdminPerm = false
        )

        return save(guildSettings)
    }
}
