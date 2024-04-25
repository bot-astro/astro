package space.astro.api.central.models.dashboard.body

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import space.astro.shared.core.util.validation.ValidationResult

@JsonIgnoreProperties(ignoreUnknown = true)
data class GuildDataSettingsBody(
    val allowMissingAdminPerm: Boolean
) {
    fun validate(): ValidationResult {
        return ValidationResult.valid()
    }
}