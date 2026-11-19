package space.astro.shared.core.models.database

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * A change proposed by the AI assistant and applied by a user
 *
 * @param action name of the tool that created the proposal
 */
@Document(collection = "ai_audit")
data class AiAuditEntity(
    @Id
    val id: ObjectId = ObjectId.get(),
    @Indexed(name = "guildId_1")
    val guildId: String,
    val userId: String,
    val surface: AiSurface,
    val proposalId: String,
    val action: String,
    val diff: List<AiDiffEntry>,
    val timestamp: Long = System.currentTimeMillis(),
)

data class AiDiffEntry(
    val field: String,
    val before: String?,
    val after: String?,
)
