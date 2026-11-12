package space.astro.api.central.models.requests

import space.astro.shared.core.utils.extensions.isValidSnowflake
import space.astro.shared.core.utils.validation.Validatable
import space.astro.shared.core.utils.validation.ValidationResult
import space.astro.shared.core.utils.validation.asValidationResult

data class InterfaceCreateBody(
    val channelId: String,
) : Validatable {
    override fun validate(): ValidationResult =
        channelId.isValidSnowflake().asValidationResult("The provided channel id is not a valid Discord id")
}
