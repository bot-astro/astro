package space.astro.shared.core.models.database.guildSettings

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import space.astro.shared.core.utils.validation.ValidationResult
import space.astro.shared.core.utils.validation.asValidationResult

@Document(collection = "guilds")
data class GuildSettingsEntity(
    @Id
    val id: ObjectId = ObjectId.get(),
    @Indexed
    val guildID: String,
    var upgradedByUserID: String? = null,
    val entitlements: MutableList<GuildEntitlementData> = mutableListOf(),
    var bannedCommands: MutableList<String> = mutableListOf(),
    val templates: MutableList<TemplateData> = mutableListOf(),
    val connections: MutableList<ConnectionData> = mutableListOf(),
    val generators: MutableList<GeneratorData> = mutableListOf(),
    var interfaces: MutableList<InterfaceData> = mutableListOf(),
    var errorLogsChannelId: String? = null,
    var allowMissingAdminPerm: Boolean = false,
)



