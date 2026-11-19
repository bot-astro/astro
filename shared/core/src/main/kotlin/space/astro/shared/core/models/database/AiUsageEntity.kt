package space.astro.shared.core.models.database

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * One turn of the AI assistant, stored for traceability and cost tracking
 *
 * @param cost cost of the turn in USD as reported by OpenRouter, null when not reported
 */
@Document(collection = "ai_usage")
data class AiUsageEntity(
    @Id
    val id: ObjectId = ObjectId.get(),
    @Indexed(name = "guildId_1")
    val guildId: String,
    val userId: String,
    val surface: AiSurface,
    val model: String,
    val inputTokens: Int,
    val outputTokens: Int,
    val cost: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
)

enum class AiSurface {
    DASHBOARD,
    DISCORD,
}
