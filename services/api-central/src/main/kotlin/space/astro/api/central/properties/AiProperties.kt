package space.astro.api.central.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * @param openrouterBaseUrl base URL of the OpenRouter API, any API compatible with it works
 * @param model OpenRouter id of the model used by the assistant
 * @param internalToken shared secret of the internal endpoints called by the bot, empty disables them
 * @param maxSteps maximum number of model calls in a single turn, each tool round is one step
 */
@ConfigurationProperties("ai")
data class AiProperties(
    val openrouterApiKey: String,
    val openrouterBaseUrl: String,
    val model: String,
    val internalToken: String,
    val maxSteps: Int,
    val monthlyMessagesFree: Long,
    val monthlyMessagesPremium: Long,
    val monthlyTokensFree: Long,
    val monthlyTokensPremium: Long,
)
