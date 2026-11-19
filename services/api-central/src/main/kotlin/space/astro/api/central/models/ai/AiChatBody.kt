package space.astro.api.central.models.ai

import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming

/**
 * Request sent by the AI SDK `useChat`, the history is kept on the server so only the last user message is used
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class AiChatBody(
    val id: String?,
    val messages: List<Message>,
) {
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
    data class Message(
        val role: String,
        val parts: List<Part> = emptyList(),
    )

    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
    data class Part(
        val type: String,
        val text: String? = null,
    )

    fun lastUserText(): String? =
        messages
            .lastOrNull { it.role == "user" }
            ?.parts
            ?.filter { it.type == "text" }
            ?.mapNotNull { it.text }
            ?.joinToString("\n")
            ?.takeIf { it.isNotBlank() }
}
