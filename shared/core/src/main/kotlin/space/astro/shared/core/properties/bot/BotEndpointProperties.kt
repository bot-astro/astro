package space.astro.shared.core.properties.bot

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("bot.endpoint")
data class BotEndpointProperties(
    val localhost: Boolean,
    val port: Long,
    val podName: String,
    val serviceName: String,
    val namespace: String,
)