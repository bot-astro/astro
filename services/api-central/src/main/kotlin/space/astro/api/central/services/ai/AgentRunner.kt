package space.astro.api.central.services.ai

import space.astro.api.central.models.ai.AiProposal

/**
 * The agent loop of the assistant, kept behind an interface so the implementation can be swapped
 */
interface AgentRunner {
    /**
     * Runs a turn of the conversation: the model answers and calls tools until it is done.
     * The history of the session is loaded and saved by the runner.
     *
     * @param onEvent called while the turn runs, for streaming
     */
    fun runTurn(
        turn: AiTurn,
        onEvent: (AiTurnEvent) -> Unit,
    ): AiTurnResult

    /**
     * Single model call without tools nor history
     */
    fun complete(
        systemPrompt: String,
        userPrompt: String,
    ): AiCompletion
}

/**
 * @param sessionKey Redis key of the conversation history
 */
data class AiTurn(
    val context: AiToolContext,
    val sessionKey: String,
    val text: String,
)

sealed interface AiTurnEvent {
    data object StepStart : AiTurnEvent

    data object StepFinish : AiTurnEvent

    data class TextDelta(
        val text: String,
    ) : AiTurnEvent

    /**
     * @param input JSON arguments of the call
     */
    data class ToolCall(
        val id: String,
        val name: String,
        val input: String,
    ) : AiTurnEvent

    data class ToolResult(
        val id: String,
        val output: String,
    ) : AiTurnEvent

    data class Proposal(
        val proposal: AiProposal,
    ) : AiTurnEvent
}

data class AiTurnResult(
    val text: String,
    val proposals: List<AiProposal>,
    val model: String,
    val inputTokens: Int,
    val outputTokens: Int,
)

data class AiCompletion(
    val text: String,
    val model: String,
    val inputTokens: Int,
    val outputTokens: Int,
)
