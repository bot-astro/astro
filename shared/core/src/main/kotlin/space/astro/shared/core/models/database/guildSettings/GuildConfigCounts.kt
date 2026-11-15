package space.astro.shared.core.models.database.guildSettings

/**
 * Size of the configuration of a guild, computed by Mongo so that the settings documents don't have to be loaded.
 * Holds what is needed to tell if the guild is premium.
 */
data class GuildConfigCounts(
    val guildID: String,
    val upgradedByUserID: String?,
    val entitlements: List<GuildEntitlementData> = emptyList(),
    val generators: Int,
    val interfaces: Int,
    val connections: Int,
    val templates: Int,
)
