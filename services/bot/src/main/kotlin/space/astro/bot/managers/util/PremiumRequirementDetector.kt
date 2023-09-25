package space.astro.bot.managers.util

import org.springframework.stereotype.Component
import space.astro.bot.config.ApplicationFeaturesConfig
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.shared.core.models.database.GuildData

@Component
class PremiumRequirementDetector(
    val discordApplicationConfig: DiscordApplicationConfig,
    val applicationFeaturesConfig: ApplicationFeaturesConfig
) {
    fun isGuildPremium(guildData: GuildData): Boolean {
        val now = System.currentTimeMillis()

        return guildData.upgradedByUserID != null
                || guildData.entitlements.any { (it.endsAt == null || it.endsAt!! >= now) && it.skuId == discordApplicationConfig.premiumServerSkuId }
    }

    fun exceededMaximumGeneratorAmount(guildData: GuildData): Boolean {
        return applicationFeaturesConfig.premiumRestrictions
                && guildData.generators.size > 2
                && !isGuildPremium(guildData)
    }
}