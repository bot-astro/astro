package space.astro.api.central.models.requests

import space.astro.shared.core.lang.L
import space.astro.shared.core.utils.validation.Validatable
import space.astro.shared.core.utils.validation.ValidationResult

data class GuildSettingsUpdateBody(
    val allowMissingAdminPerms: Boolean,
    // Language + region
    val locale: String,
): Validatable {
    override fun validate(): ValidationResult {
        if (!L.isLocaleSupported(locale))
            return ValidationResult.invalid("Unsupported locale $locale")

        return ValidationResult.valid()
    }
}
