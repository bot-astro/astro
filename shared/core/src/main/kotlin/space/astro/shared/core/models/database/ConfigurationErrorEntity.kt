package space.astro.shared.core.models.database

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

/**
 * An error that happened because of the configuration of a guild
 *
 * Only the [code] and the values needed to describe it ([args]) are stored,
 * the text is computed from the translations when the error is shown
 */
@Document(collection = "errors")
data class ConfigurationErrorEntity(
    @Id
    val id: ObjectId = ObjectId.get(),
    @Indexed
    val guildId: String,
    val code: ConfigurationErrorCode,
    val args: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
) {
    enum class Guide {
        BASIC,
        GENERATOR,
        TEMPLATE,
        INTERFACE,
        VOICE_ROLE,
    }
}
