package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("discord.api")
data class DiscordApiProperties(
    val enabled: Boolean,
    val baseUrl: String
)