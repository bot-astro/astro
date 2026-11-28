package space.astro.api.central.components

import org.springframework.stereotype.Component
import space.astro.shared.core.exceptions.ABadRequestException
import space.astro.shared.core.utils.validation.Validatable
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode

/**
 * Applies a JSON Merge Patch (RFC 7396) to an object: objects are merged field by field, a null removes the field
 * (so it falls back to its default), anything else, lists included, replaces the current value.
 */
@Component
class MergePatchHelper(
    private val jsonMapper: JsonMapper,
) {
    // unknown fields would otherwise be ignored and a typo like `userLimit` for `user_limit` would silently do nothing
    private val strictJsonMapper = jsonMapper.rebuild().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build()

    /**
     * @return the validated result of applying [patch] to [current], [current] is not modified
     * @throws ABadRequestException if the patch is not an object or the result is not valid
     */
    fun <T : Any> apply(
        current: T,
        patch: JsonNode,
        type: Class<T>,
    ): T {
        if (!patch.isObject) {
            throw ABadRequestException("The body must be a JSON object with the fields to change.")
        }

        val result = strictJsonMapper.treeToValue(merge(jsonMapper.valueToTree(current), patch), type)
        (result as? Validatable)?.validate()?.throwIfInvalid()
        return result
    }

    private fun merge(
        target: JsonNode?,
        patch: JsonNode,
    ): JsonNode {
        if (!patch.isObject) return patch

        val result = (target as? ObjectNode)?.deepCopy() ?: jsonMapper.createObjectNode()
        patch.properties().forEach { (name, value) ->
            if (value.isNull) {
                result.remove(name)
            } else {
                result.set(name, merge(result.get(name), value))
            }
        }
        return result
    }
}
