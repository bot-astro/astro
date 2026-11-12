package space.astro.support.bot.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("support.server")
data class SupportServerProperties(
    val guildId: String,
    val premiumRoleId: String,
    val premiumSkuId: String,
    /** Application whose user entitlements for [premiumSkuId] also grant the premium role */
    val entitlementsApplicationId: String,
    val entitlementsApplicationToken: String,
)
