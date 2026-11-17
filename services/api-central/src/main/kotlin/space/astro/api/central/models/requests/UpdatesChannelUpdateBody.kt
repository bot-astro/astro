package space.astro.api.central.models.requests

import space.astro.shared.core.utils.extensions.isValidSnowflake
import space.astro.shared.core.utils.validation.Validatable
import space.astro.shared.core.utils.validation.ValidationResult
import space.astro.shared.core.utils.validation.asValidationResult

/**
 * @param channelId the channel to use, null disables the updates channel
 * @param createChannel creates a new channel and uses it, [channelId] is ignored
 */
data class UpdatesChannelUpdateBody(
    val channelId: String?,
    val createChannel: Boolean = false,
) : Validatable {
    override fun validate(): ValidationResult =
        (channelId == null || channelId.isValidSnowflake()).asValidationResult("The provided channel id is not a valid Discord id")
}
