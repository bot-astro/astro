package space.astro.shared.core.models.database.guildSettings

import space.astro.shared.core.utils.extensions.isValidSnowflake
import space.astro.shared.core.utils.validation.Validatable
import space.astro.shared.core.utils.validation.ValidationResult
import space.astro.shared.core.utils.validation.asValidationResult

data class ConnectionData(
    var id: String,
    var roleID: String,
    var action: ConnectionAction = ConnectionAction.ASSIGN,
    var permanent: Boolean = false
): Validatable {
    override fun validate(): ValidationResult {
        val channelValidation = channelId.isValidSnowflake().asValidationResult("Channel id is not a valid Discord id")
        val roleValidation = roleId.isValidSnowflake().asValidationResult("Role id is not a valid Discord id")

        return ValidationResult.combine(channelValidation, roleValidation)
    }

    enum class ConnectionAction {
        ASSIGN, REMOVE, TOGGLE;
    }
}