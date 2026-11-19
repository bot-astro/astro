package space.astro.api.central.models.ai

import com.fasterxml.jackson.annotation.JsonIgnore
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * @param resetsAt ISO instant of the start of the next month (UTC), when the counters reset
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class AiUsageResponse(
    val messagesUsed: Long,
    val messagesLimit: Long,
    val tokensUsed: Long,
    val tokensLimit: Long,
    val premium: Boolean,
    val resetsAt: String,
) {
    @get:JsonIgnore
    val limitReached: Boolean
        get() = messagesUsed >= messagesLimit || tokensUsed >= tokensLimit
}
