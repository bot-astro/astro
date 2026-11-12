package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("discord.oauth")
data class DiscordOAuthProperties(
    val id: String,
    val secret: String,
    val redirectUri: String,
)
