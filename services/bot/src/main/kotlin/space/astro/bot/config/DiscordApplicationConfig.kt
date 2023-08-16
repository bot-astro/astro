package space.astro.bot.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("discord.application")
class DiscordApplicationConfig {
    var botId: Long = 1006899610624741466
    var token: String = "token"
    var activityType = "WATCHING"
    var activityText = "new infrastructure"
    var commandGuilds = emptyList<Long>()
    var whitelistedGuilds = emptyList<Long>()
}
