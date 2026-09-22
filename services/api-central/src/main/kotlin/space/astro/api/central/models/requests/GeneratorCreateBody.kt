package space.astro.api.central.models.requests

import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import space.astro.shared.core.utils.extensions.isValidSnowflake
import space.astro.shared.core.utils.validation.Validatable
import space.astro.shared.core.utils.validation.ValidationResult
import space.astro.shared.core.utils.validation.asValidationResult

data class GeneratorCreateBody(
    val name: String,
    val categoryId: String?,
): Validatable {
    override fun validate(): ValidationResult {
        val nameValidation = (name.isNotBlank() && name.length in 1..VoiceChannel.MAX_NAME_LENGTH)
            .asValidationResult("Name must be between 1 and ${VoiceChannel.MAX_NAME_LENGTH} characters long")
        val categoryValidation = categoryId?.isValidSnowflake()?.asValidationResult("Category id is not a valid Discord id") ?: ValidationResult.valid()

        return ValidationResult.combine(nameValidation, categoryValidation)
    }
}
