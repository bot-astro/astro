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
    var upgradedByUserID: String?,
    val entitlements: MutableList<GuildEntitlementData>,
    val templates: MutableList<TemplateData>,
    val connections: MutableList<ConnectionData>,
    val generators: MutableList<GeneratorData>,
    var interfaces: MutableList<InterfaceData>,
    var allowMissingAdminPerm: Boolean,
)



