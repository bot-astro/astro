package space.astro.bot.components.managers

import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VariablesManager
import space.astro.bot.config.ApplicationFeaturesConfig
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.shared.core.models.database.GuildData
import space.astro.shared.core.models.database.GuildEntitlement

@Component
class PremiumRequirementDetector(
    val discordApplicationConfig: DiscordApplicationConfig,
    val applicationFeaturesConfig: ApplicationFeaturesConfig,
) {
    private fun isEntitlementActive(
        entitlement: GuildEntitlement,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        return entitlement.endsAt?.takeUnless { it > currentTimeMillis } == null
    }

    fun isGuildPremium(guildData: GuildData): Boolean {
        if (!applicationFeaturesConfig.premiumRestrictions) {
            return true
        }

        val currentMillis = System.currentTimeMillis()

        return guildData.entitlements.any {
            isEntitlementActive(it, currentMillis) && it.skuId == discordApplicationConfig.premiumServerSkuId
        }
    }

    fun exceededMaximumGeneratorAmount(guildData: GuildData): Boolean {
        return applicationFeaturesConfig.premiumRestrictions
                && guildData.generators.size > 2
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
}