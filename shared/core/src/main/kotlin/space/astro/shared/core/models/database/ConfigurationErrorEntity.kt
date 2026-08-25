package space.astro.shared.core.models.database

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document


@Document(collection = "errors")
data class ConfigurationErrorEntity(
    @Id
    val id: ObjectId = ObjectId.get(),
    @Indexed
    val guildId: String,
    val description: String,
    val premiumRequired: Boolean = false,
    val guide: Guide? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    enum class Guide {
        BASIC,
        GENERATOR,
        TEMPLATE,
        INTERFACE,
        VOICE_ROLE
    }
}