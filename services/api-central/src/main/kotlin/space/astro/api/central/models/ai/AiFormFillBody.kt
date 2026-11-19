package space.astro.api.central.models.ai

import tools.jackson.databind.JsonNode
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.databind.annotation.JsonNaming
import tools.jackson.databind.node.ObjectNode

/**
 * @param page settings page the form belongs to: generator, interface, template or voice_role
 * @param currentValues the form state, with the same field names as the settings DTOs
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class AiFormFillBody(
    val page: String,
    val entityId: String? = null,
    val currentValues: ObjectNode,
    val prompt: String,
) {
    companion object {
        val PAGES = setOf("generator", "interface", "template", "voice_role")
    }
}

/**
 * @param values same shape as the current values, every changed top-level key is replaced whole
 * @param changedFields top-level keys or dotted paths of the changed values
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class AiFormFillResponse(
    val values: JsonNode,
    val changedFields: List<String>,
    val message: String,
)
