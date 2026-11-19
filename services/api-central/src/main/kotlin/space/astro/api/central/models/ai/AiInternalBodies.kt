package space.astro.api.central.models.ai

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class AiInternalTurnBody(
    val userId: String,
    val threadId: String,
    val text: String,
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class AiInternalTurnResponse(
    val text: String,
    val proposals: List<AiProposalDto>,
    val limitReached: Boolean,
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class AiInternalProposalBody(
    val userId: String,
)

/**
 * @param result the guild settings after the change
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class AiProposalApplyResponse(
    val status: String,
    val result: Any,
)
