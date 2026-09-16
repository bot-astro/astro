package space.astro.shared.core.models.database.guildSettings

import space.astro.shared.core.utils.extensions.isValidSnowflake
import space.astro.shared.core.utils.validation.ValidationResult
import space.astro.shared.core.utils.validation.asValidationResult

data class ConnectionData(
    var id: String,
    var roleID: String,
    var action: ConnectionAction = ConnectionAction.ASSIGN,
    var permanent: Boolean = false
) {
    fun validate() : ValidationResult {
        val idValidation = id.isValidSnowflake().asValidationResult("invalid channel id")
        val roleIDValidation = roleID.isValidSnowflake().asValidationResult("invalid role id")

        return ValidationResult.combine(
            idValidation,
            roleIDValidation
        )
    }

    enum class ConnectionAction {
        ASSIGN, REMOVE, TOGGLE;
    }
}