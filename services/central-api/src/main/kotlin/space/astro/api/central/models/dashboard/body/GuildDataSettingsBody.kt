package space.astro.api.central.models.dashboard.body

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GuildDataSettingsBody(
    val allowMissingAdminPerm: Boolean
)