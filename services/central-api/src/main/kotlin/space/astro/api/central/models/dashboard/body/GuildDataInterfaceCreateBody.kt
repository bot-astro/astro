package space.astro.api.central.models.dashboard.body

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import space.astro.shared.core.util.extention.asValidationResult
import space.astro.shared.core.util.extention.isValidSnowflake
import space.astro.shared.core.util.validation.ValidationResult

@JsonIgnoreProperties(ignoreUnknown = true)
data class GuildDataInterfaceCreateBody(
    val channelID: String
) {
    fun validate() : ValidationResult {
        return channelID.isValidSnowflake().asValidationResult("the provided channel is not valid")
    }
}