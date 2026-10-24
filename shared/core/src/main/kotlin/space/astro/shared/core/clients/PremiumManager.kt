package space.astro.shared.core.clients

import space.astro.shared.core.models.database.guildSettings.GuildEntitlementData
import space.astro.shared.core.models.database.guildSettings.GuildSettingsEntity
import space.astro.shared.core.properties.PremiumProperties

/**
 * Decides which features and how many configurations a guild can use, based on its premium status
 */
class PremiumManager(
    private val premiumProperties: PremiumProperties,
) {
    companion object {
        const val FREE_MAX_CONNECTIONS = 1
        const val FREE_MAX_GENERATORS = 2
        const val FREE_MAX_INTERFACES = 1
        const val FREE_MAX_TEMPLATES = 3

        /**
         * The runtime checks are more tolerant than the creation ones,
         * so that guilds that just lost premium keep working until they clean up their configuration
         */
        private const val EXCEEDED_MAX_CONNECTIONS = 1
        private const val EXCEEDED_MAX_GENERATORS = 3

        private val premiumVariablesRegex = """\{(n|nato|roman|activity_[^\s\\}]+)}""".toRegex()
    }

    fun isGuildPremium(guildSettings: GuildSettingsEntity): Boolean {
        if (!premiumProperties.restrictions) {
            return true
        }

        if (guildSettings.upgradedByUserID != null) {
            return true
        }

        val now = System.currentTimeMillis()

        return guildSettings.entitlements.any {
            it.isActive(now) && it.skuId == premiumProperties.serverSkuId
        }
    }

    private fun GuildEntitlementData.isActive(now: Long) = endsAt == null || endsAt > now

    // ////////////
    // / LIMITS ///
    // ////////////

    fun canCreateConnection(guildSettings: GuildSettingsEntity) =
        isGuildPremium(guildSettings) || guildSettings.connections.size < FREE_MAX_CONNECTIONS

    fun canCreateGenerator(guildSettings: GuildSettingsEntity) =
        isGuildPremium(guildSettings) || guildSettings.generators.size < FREE_MAX_GENERATORS

    fun canCreateInterface(guildSettings: GuildSettingsEntity) =
        isGuildPremium(guildSettings) || guildSettings.interfaces.size < FREE_MAX_INTERFACES

    fun canCreateTemplate(guildSettings: GuildSettingsEntity) =
        isGuildPremium(guildSettings) || guildSettings.templates.size < FREE_MAX_TEMPLATES

    fun exceededMaximumConnectionsAmount(guildSettings: GuildSettingsEntity) =
        guildSettings.connections.size > EXCEEDED_MAX_CONNECTIONS && !isGuildPremium(guildSettings)

    fun exceededMaximumGeneratorAmount(guildSettings: GuildSettingsEntity) =
        guildSettings.generators.size > EXCEEDED_MAX_GENERATORS && !isGuildPremium(guildSettings)

    // ////////////
    // / NAMES ///
    // ////////////

    fun canUseVCNameTemplate(
        guildSettings: GuildSettingsEntity,
        vcNameTemplate: String,
    ) = isGuildPremium(guildSettings) || !containsPremiumVariable(vcNameTemplate)

    fun containsPremiumVariable(template: String) = premiumVariablesRegex.containsMatchIn(template)

    // ///////////////
    // / FEATURES ///
    // ///////////////

    fun canUseFallbackGenerator(guildSettings: GuildSettingsEntity) = isGuildPremium(guildSettings)

    fun canCreatePrivateChatOnVCGeneration(guildSettings: GuildSettingsEntity) = isGuildPremium(guildSettings)

    fun canCreateWaitingRoomOnVCGeneration(guildSettings: GuildSettingsEntity) = isGuildPremium(guildSettings)

    fun canSendMessageInVCChatOnVCGeneration(guildSettings: GuildSettingsEntity) = isGuildPremium(guildSettings)

    fun canAssignTemporaryVCOwnerRole(guildSettings: GuildSettingsEntity) = isGuildPremium(guildSettings)

    fun canEditInterfaceMessage(guildSettings: GuildSettingsEntity) = isGuildPremium(guildSettings)

    fun canEditInterfaceButtonOrder(guildSettings: GuildSettingsEntity) = isGuildPremium(guildSettings)
}
