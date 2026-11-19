package space.astro.bot.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Where the AI assistant of api-central is reachable
 *
 * @param internalToken Value of the internal auth header, it must match the token api-central expects on its internal AI endpoints
 * @param timeoutSeconds Read timeout, a turn can take a while because the model may call several tools
 */
@ConfigurationProperties("ai.assistant")
data class AiAssistantProperties(
    val baseUrl: String,
    val internalToken: String,
    val timeoutSeconds: Long = 120,
)
