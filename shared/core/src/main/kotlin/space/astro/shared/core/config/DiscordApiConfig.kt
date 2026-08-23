package space.astro.shared.core.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("discord.api")
data class DiscordApiConfig(
    val baseUrl: String
)