package space.astro.entitlements.expiration.job.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties("discord.application")
class DiscordApplicationConfig {
    var id: Long = 1176957574332026940
    var token: String = "token"
}