package space.astro.shared.core.models.database.guildSettings

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.dv8tion.jda.api.Region
import space.astro.shared.core.utils.extensions.isValidSnowflake
import space.astro.shared.core.utils.validation.ValidationResult
import space.astro.shared.core.utils.validation.asValidationResult

data class TemplateData(
    var id: String = NanoIdUtils.randomNanoId(),
    var name: String,
    var enabledGeneratorIds: MutableList<String>? = null,
    var vcName: String? = null,
    var vcLimit: Int? = null,
    var vcBitrate: Int? = null,
    var vcRegion: String? = null
) {
    fun validate() : ValidationResult {
        val nameValidation = name.isNotEmpty().asValidationResult("template name cannot be empty")
        val enabledGeneratorIdsValidation = (enabledGeneratorIds?.all { it.isValidSnowflake() } ?: true).asValidationResult("invalid generator in the enabled generators")
        val vcNameValidation = (vcName?.length?.let { it in 2..500 } ?: true).asValidationResult("the voice channel name for the template must be between 2 and 500 characters")
        val vcLimitValidation = (vcLimit?.let { it in 0..99 } ?: true).asValidationResult("the voice channel limit for the template must be between 0 and 99")
        val vcBitrateValidation = (vcBitrate?.let { it in 8000..384000 } ?: true).asValidationResult("the voice channel bitrate for the template must be between 8000 and 384000")
        val vcRegionValidation = (vcRegion?.let { it in Region.values().map { region -> region.key } } ?: true).asValidationResult("the voice channel region for the template is not a valid region")

        return ValidationResult.combine(
            nameValidation,
            enabledGeneratorIdsValidation,
            vcNameValidation,
            vcLimitValidation,
            vcBitrateValidation,
            vcRegionValidation
        )
    }
}