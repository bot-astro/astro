package space.astro.support.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("discord.application")
class DiscordApplicationConfig {
    var botId: Long = 1176957574332026940
    var token: String = "token"
    var activityType = "WATCHING"
    var activityText = "over the Astro community"
    var commandGuilds = listOf(700607091391594567)
    var whitelistedGuilds = listOf(700607091391594567)
    var entitlementsBotId: Long = 715621848489918495
    var entitlementsBotToken: String = "entitlements_token"
    var guildIdForPremiumRole: Long = 700607091391594567
    var premiumSkuId: Long = 1096107722115661934
    var premiumRoleId: Long = 761283474241093682
}