package space.astro.shared.core.repositories.mongo

import org.springframework.data.mongodb.repository.MongoRepository
import space.astro.shared.core.lang.L
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity

interface GuildSettingsRepository : MongoRepository<GuildSettingsEntity, String> {
    // `first` so that a duplicated guild document doesn't make every lookup of that guild throw
    fun findFirstByGuildID(guildID: String): GuildSettingsEntity?

    fun findByGuildID(guildID: String): GuildSettingsEntity? = findFirstByGuildID(guildID)

    fun createNewGuildSettings(
        guildID: String,
        locale: String,
    ): GuildSettingsEntity {
        val guildSettings =
            GuildSettingsEntity(
                guildID = guildID,
                upgradedByUserID = null,
                entitlements = mutableListOf(),
                templates = mutableListOf(),
                connections = mutableListOf(),
                interfaces = mutableListOf(),
                generators = mutableListOf(),
                allowMissingAdminPerm = false,
                locale = L.closestSupportedLocale(locale).toLanguageTag(),
            )

        return save(guildSettings)
    }
}
