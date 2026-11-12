package space.astro.shared.core.properties.support_bot

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Where the support bot API is reachable, for the services that call it
 *
 * @param auth Value of the Authorization header, it must match `support.bot.api.auth` of the support bot
 */
@ConfigurationProperties("support.bot.endpoint")
data class SupportBotEndpointProperties(
    val enabled: Boolean,
    val baseUrl: String,
    val auth: String,
)
