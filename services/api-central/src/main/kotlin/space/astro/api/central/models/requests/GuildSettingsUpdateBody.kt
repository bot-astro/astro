package space.astro.api.central.models.requests

import io.swagger.v3.oas.annotations.media.Schema
import space.astro.shared.core.lang.L
import space.astro.shared.core.utils.validation.Validatable
import space.astro.shared.core.utils.validation.ValidationResult

data class GuildSettingsUpdateBody(
    val allowMissingAdminPerms: Boolean,
    // Language + region
    @field:Schema(description = "BCP 47 language tag with the region, one of the languages the bot supports", example = "en-US")
    val locale: String,
) : Validatable {
    override fun validate(): ValidationResult {
        if (!L.isLocaleSupported(locale)) {
            return ValidationResult.invalid("Unsupported locale $locale")
        }

        return ValidationResult.valid()
    }
}
