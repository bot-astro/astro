package space.astro.api.central.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("discord.application")
data class DiscordApplicationProperties(
    val id: String,
    val token: String,
)
