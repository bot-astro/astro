package space.astro.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("discord.application")
class DiscordApplicationConfig {
    var botId: Long = 715621848489918495
    var token: String = "token"
    var activityType = "WATCHING"
    var activityText = "new infrastructure"
    var commandGuilds = emptyList<Long>()
    var whitelistedGuilds = emptyList<Long>()
    var premiumServerSkuId: String = "sku_id"
}
