package space.astro.shared.core.components.managers

import org.springframework.stereotype.Component
import space.astro.shared.core.configs.PremiumFeaturesConfig
import space.astro.shared.core.models.database.GuildData
import space.astro.shared.core.models.database.GuildEntitlement

@Component
class PremiumRequirementDetector(
    private val premiumFeaturesConfig: PremiumFeaturesConfig
) {
    private fun isEntitlementActive(
        entitlement: GuildEntitlement,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return entitlement.endsAt?.takeUnless { it > currentTimeMillis } == null
    }

    fun isGuildPremium(guildData: GuildData): Boolean {
        if (!premiumFeaturesConfig.restrictions) {
            return true
        }

        if (guildData.upgradedByUserID != null) {
            return true
        }

        val currentMillis = System.currentTimeMillis()

        return guildData.entitlements.any {
            isEntitlementActive(it, currentMillis) && it.skuId == premiumFeaturesConfig.serverSkuId
        }
    }

    fun canCreateConnection(guildData: GuildData): Boolean {
        if (isGuildPremium(guildData) || !premiumFeaturesConfig.restrictions) {
            return true
        }

        return guildData.connections.size < 1
    }

    fun canCreateGenerator(guildData: GuildData): Boolean {
        if (isGuildPremium(guildData) || !premiumFeaturesConfig.restrictions) {
            return true
        }

        return guildData.generators.size < 2
    }

    fun canCreateInterface(guildData: GuildData): Boolean {
        if (isGuildPremium(guildData) || !premiumFeaturesConfig.restrictions) {
            return true
        }

        return guildData.interfaces.size < 1
    }

    fun canCreateTemplate(guildData: GuildData): Boolean {
        if (isGuildPremium(guildData) || !premiumFeaturesConfig.restrictions) {
            return true
        }

        return guildData.templates.size < 3
    }

    fun exceededMaximumConnectionsAmount(guildData: GuildData): Boolean {
        return premiumFeaturesConfig.restrictions
                && guildData.connections.size > 1
                && !isGuildPremium(guildData)
    }

    fun exceededMaximumGeneratorAmount(guildData: GuildData): Boolean {
        return premiumFeaturesConfig.restrictions
                && guildData.generators.size > 3
                && !isGuildPremium(guildData)
    }

    fun canUseVCNameTemplate(guildData: GuildData, vcNameTemplate: String): Boolean {
        if (isGuildPremium(guildData)) {
            return true
        }

        if (VariablesManager.Checkers.containsPremiumVariable(vcNameTemplate)) {
            return false
        }

        return true
    }

    fun canValidateBadwords(guildData: GuildData): Boolean {
        return isGuildPremium(guildData)
    }

    fun canUseFallbackGenerator(guildData: GuildData) = isGuildPremium(guildData)
    fun canCreatePrivateChatOnVCGeneration(guildData: GuildData) = isGuildPremium(guildData)
    fun canCreateWaitingRoomOnVCGeneration(guildData: GuildData) = isGuildPremium(guildData)
    fun canSendMessageInVCChatOnVCGeneration(guildData: GuildData) = isGuildPremium(guildData)
    fun canAssignTemporaryVCOwnerRole(guildData: GuildData) = isGuildPremium(guildData)

    fun canEditInterfaceMessage(guildData: GuildData) = isGuildPremium(guildData)
    fun canEditInterfaceButtonOrder(guildData: GuildData) = isGuildPremium(guildData)
}