package space.astro.api.central.models.requests

import space.astro.shared.core.utils.validation.ValidationResult

data class GuildSettingsUpdateBody(
    val allowMissingAdminPerms: Boolean,
    // Language + region
    val locale: String,
) {
    fun validate(): ValidationResult {
        // TODO: validate locale key®
        return ValidationResult.valid()
    }
}
