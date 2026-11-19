package space.astro.api.central.services.ai

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import space.astro.api.central.models.ai.AiProposal
import space.astro.shared.core.models.database.AiDiffEntry
import space.astro.shared.core.utils.redis.RedisKey
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.readValue
import java.security.MessageDigest
import java.time.Duration
import java.time.Instant
import java.util.HexFormat

/**
 * Stores the proposals of the assistant until a user applies or cancels them
 */
@Service
class AiProposalService(
    private val redis: StringRedisTemplate,
    private val jsonMapper: JsonMapper,
) {
    companion object {
        val PROPOSAL_TTL: Duration = Duration.ofMinutes(15)

        // the key outlives the proposal so that an expired proposal can be told apart from a missing one
        private val KEY_GRACE: Duration = Duration.ofHours(1)
    }

    fun create(
        context: AiToolContext,
        action: String,
        summary: String,
        targetId: String?,
        payload: Any?,
        diff: List<AiDiffEntry>,
        baseVersion: String?,
    ): AiProposal {
        val proposal =
            AiProposal(
                id = NanoIdUtils.randomNanoId(),
                guildId = context.guildId,
                userId = context.userId,
                surface = context.surface,
                action = action,
                summary = summary,
                targetId = targetId,
                payload = payload?.let { jsonMapper.writeValueAsString(it) },
                diff = diff,
                baseVersion = baseVersion,
                expiresAt = Instant.now().plus(PROPOSAL_TTL).toEpochMilli(),
            )

        redis.opsForValue().set(
            RedisKey.AI_PROPOSAL(proposal.id),
            jsonMapper.writeValueAsString(proposal),
            PROPOSAL_TTL.plus(KEY_GRACE),
        )

        return proposal
    }

    fun get(proposalId: String): AiProposal? =
        redis
            .opsForValue()
            .get(RedisKey.AI_PROPOSAL(proposalId))
            ?.let { jsonMapper.readValue(it) }

    fun delete(proposalId: String) {
        redis.delete(RedisKey.AI_PROPOSAL(proposalId))
    }

    /**
     * Hash of the serialized [target], null when there is no target
     */
    fun version(target: Any?): String? =
        target?.let {
            HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(jsonMapper.writeValueAsBytes(it)),
            )
        }

    /**
     * Field by field difference of two objects, nested values are compared and shown as JSON.
     * A null [before] lists every set field of [after] (creation), a null [after] the opposite (deletion)
     */
    fun diff(
        before: Any?,
        after: Any?,
    ): List<AiDiffEntry> {
        val beforeNode: JsonNode? = before?.let { jsonMapper.valueToTree(it) }
        val afterNode: JsonNode? = after?.let { jsonMapper.valueToTree(it) }
        val fields = (beforeNode?.propertyNames().orEmpty() + afterNode?.propertyNames().orEmpty()).distinct()

        return fields.mapNotNull { field ->
            val beforeValue = beforeNode?.get(field)?.takeUnless { it.isNull }
            val afterValue = afterNode?.get(field)?.takeUnless { it.isNull }
            if (beforeValue == afterValue) {
                null
            } else {
                AiDiffEntry(field, beforeValue?.asDisplayString(), afterValue?.asDisplayString())
            }
        }
    }

    private fun JsonNode.asDisplayString(): String = if (isString) asString() else toString()
}
