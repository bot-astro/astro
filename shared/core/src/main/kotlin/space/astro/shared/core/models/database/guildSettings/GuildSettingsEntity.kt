package space.astro.shared.core.models.database.guildSettings

import org.bson.types.ObjectId
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.util.*

@Document(collection = "guilds")
data class GuildSettingsEntity(
    @Id
    val id: ObjectId = ObjectId.get(),
    @Indexed
    val guildID: String,
    var nickname: String? = null,
    var bio: String? = null,
    var avatarUrl: String? = null,
    var bannerUrl: String? = null,
    /**
     * Language + region
     */
    var locale: String = "en-US",
    var allowMissingAdminPerm: Boolean = false,
    var upgradedByUserID: String? = null,
    val entitlements: MutableList<GuildEntitlementData> = mutableListOf(),
    val templates: MutableList<TemplateData> = mutableListOf(),
    val connections: MutableList<ConnectionData> = mutableListOf(),
    val generators: MutableList<GeneratorSettings> = mutableListOf(),
    var interfaces: MutableList<InterfaceData> = mutableListOf(),
) {
    val javaLocale: Locale = Locale.forLanguageTag(locale)
}
