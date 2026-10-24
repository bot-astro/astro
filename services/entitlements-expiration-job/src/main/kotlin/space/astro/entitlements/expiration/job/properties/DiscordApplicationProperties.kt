package space.astro.entitlements.expiration.job.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("discord.application")
data class DiscordApplicationProperties(
    val id: Long,
    val token: String,
)
