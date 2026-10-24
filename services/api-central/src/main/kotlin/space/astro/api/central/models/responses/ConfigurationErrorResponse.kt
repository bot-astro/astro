package space.astro.api.central.models.responses

import space.astro.shared.core.lang.localizedDescription
import space.astro.shared.core.models.database.ConfigurationErrorCode
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import java.util.Locale

data class ConfigurationErrorResponse(
    val id: String,
    val guildId: String,
    val code: ConfigurationErrorCode,
    val description: String,
    val guide: ConfigurationErrorEntity.Guide?,
    val premiumRequired: Boolean,
    val timestamp: Long,
) {
    companion object {
        fun from(
            entity: ConfigurationErrorEntity,
            locale: Locale,
        ) = ConfigurationErrorResponse(
            id = entity.id.toHexString(),
            guildId = entity.guildId,
            code = entity.code,
            description = entity.localizedDescription(locale),
            guide = entity.code.guide,
            premiumRequired = entity.code.premiumRequired,
            timestamp = entity.timestamp,
        )
    }
}
