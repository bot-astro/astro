package space.astro.shared.core.models.database.guildSettings

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import space.astro.shared.core.utils.extensions.isValidSnowflake
import space.astro.shared.core.utils.validation.ValidationResult
import space.astro.shared.core.utils.validation.asValidationResult

data class GeneratorData(
    val id: String,
    var fallbackId: String?,
    var queueMode: Boolean,
    var defaultName: String,
    var defaultLockedName: String?,
    var defaultHiddenName: String?,
    var userLimit: Int,
    var bitrate: Int,
    var category: String?,
    var permissionsInherited: PermissionsInherited,
    var permissionsTargetRole: String?,
    var permissionsImmuneRole: String?,
    var ownerPermissions: Long,
    var ownerRole: String?,
    var initialState: VCState,
    var initialPosition: InitialPosition,
    var renameConditions: RenameConditions,
    var commandsSettings: CommandsSettings,

    var autoChat: Boolean,
    var autoWaiting: Boolean,

    var chatCategory: String?,
    var chatTopic: String?,
    var chatNsfw: Boolean,
    var chatSlowmode: Int,
    var chatPermissionsInherited: PermissionsInherited,
    var defaultChatName: String,
    var defaultChatText: String?,
    var defaultChatTextEmbed: Boolean,
    var chatInterface: Int,

    var waitingCategory: String?,
    var waitingPermissionsInherited: PermissionsInherited,
    var defaultWaitingName: String,
    var waitingBitrate: Int,
    var waitingPosition: InitialPosition,
    var waitingUserLimit: Int,

    var ownerPermissionIds: List<String>
) {
    fun parseAndValidate() : ValidationResult {
        ownerPermissions = Permission.getRaw(ownerPermissionIds.mapNotNull { try { Permission.valueOf(it) } catch(e: Exception) { null } })

        val idValidation = id.isValidSnowflake().asValidationResult("invalid generator id")
        val fallbackIdValidation = (fallbackId?.isValidSnowflake() ?: true).asValidationResult("invalid fallback generator id")
        val defaultNameValidation = (defaultName.length in 2..500).asValidationResult("the default name for the generator must be between 2 and 500 characters")
        val defaultLockedNameValidation = (defaultLockedName?.length?.let { it in 2..500 } ?: true).asValidationResult("the default locked name for the generator must be between 2 and 500 characters")
        val defaultHiddenNameValidation = (defaultHiddenName?.length?.let { it in 2..500 } ?: true).asValidationResult("the default hidden name for the generator must be between 2 and 500 characters")
        val userLimitValidation = (userLimit in 0..99).asValidationResult("the user limit for the generator must be between 0 and 99")
        val bitrateValidation = (bitrate in 0..384000).asValidationResult("the bitrate for the generator must be between 0 and 384000")
        val categoryValidation = (category?.isValidSnowflake() ?: true).asValidationResult("invalid category id")
        val permissionsTargetRoleValidation = (permissionsTargetRole?.isValidSnowflake() ?: true).asValidationResult("invalid permissions target role id")
        val permissionsImmuneRoleValidation = (permissionsImmuneRole?.isValidSnowflake() ?: true).asValidationResult("invalid permissions immune role id")
        val ownerRoleValidation = (ownerRole?.isValidSnowflake() ?: true).asValidationResult("invalid owner role id")
        val commandsSettingsValidation = commandsSettings.validate()
        val chatCategoryValidation = (chatCategory?.isValidSnowflake() ?: true).asValidationResult("invalid chat category id")
        val chatTopicValidation = (chatTopic?.length?.let { it in 0.. TextChannel.MAX_TOPIC_LENGTH } ?: true).asValidationResult("the chat topic for the generator must be between 0 and ${TextChannel.MAX_TOPIC_LENGTH} characters")
        val chatSlowmodeValidation = (chatSlowmode in 0..TextChannel.MAX_SLOWMODE).asValidationResult("the chat slowmode for the generator must be between 0 and ${TextChannel.MAX_SLOWMODE}")
        val defaultChatNameValidation = (defaultChatName.length in 2..500).asValidationResult("the default chat name for the generator must be between 2 and 500 characters")
        val maxDefaultChatTextLength = if (defaultChatTextEmbed) MessageEmbed.DESCRIPTION_MAX_LENGTH else MessageEmbed.TEXT_MAX_LENGTH
        val defaultChatTextValidation = (defaultChatText?.length?.let { it in 0..maxDefaultChatTextLength } ?: true).asValidationResult("the default chat text for the generator must be between 0 and $maxDefaultChatTextLength characters")
        val waitingCategoryValidation = (waitingCategory?.isValidSnowflake() ?: true).asValidationResult("invalid waiting room category id")
        val defaultWaitingNameValidation = (defaultWaitingName.length in 2..500).asValidationResult("the default waiting room name for the generator must be between 2 and 500 characters")
        val waitingBitrateValidation = (waitingBitrate in 0..384000).asValidationResult("the waiting room bitrate for the generator must be between 0 and 384000")
        val waitingUserLimitValidation = (waitingUserLimit in 0..99).asValidationResult("the waiting room user limit for the generator must be between 0 and 99")

        return ValidationResult.combine(
            idValidation,
            fallbackIdValidation,
            defaultNameValidation,
            defaultLockedNameValidation,
            defaultHiddenNameValidation,
            userLimitValidation,
            bitrateValidation,
            categoryValidation,
            permissionsTargetRoleValidation,
            permissionsImmuneRoleValidation,
            ownerRoleValidation,
            commandsSettingsValidation,
            chatCategoryValidation,
            chatTopicValidation,
            chatSlowmodeValidation,
            defaultChatNameValidation,
            defaultChatTextValidation,
            waitingCategoryValidation,
            defaultWaitingNameValidation,
            waitingBitrateValidation,
            waitingUserLimitValidation
        )
    }

    data class CommandsSettings(
        var maxUserLimit: Int,
        var minUserLimit: Int,

        var maxBitrate: Int?,
        var minBitrate: Int,

        var badwordsAllowed: Boolean,
    ) {
        fun validate() : ValidationResult {
            val maxUserLimitValidation = (maxUserLimit in 0..99).asValidationResult("the maximum user limit for the generator must be between 0 and 99")
            val minUserLimitValidation = (minUserLimit in 0..99).asValidationResult("the minimum user limit for the generator must be between 0 and 99")
            val maxBitrateValidation = (maxBitrate?.let { it in 8000..384000 } ?: true).asValidationResult("the maximum bitrate for the generator must be between 8000 and 384000")
            val minBitrateValidation = (minBitrate in 8000..384000).asValidationResult("the minimum bitrate for the generator must be between 8000 and 384000")

            return ValidationResult.combine(
                maxUserLimitValidation,
                minUserLimitValidation,
                maxBitrateValidation,
                minBitrateValidation
            )
        }
    }

    data class RenameConditions(
        var stateChange: Boolean,
        var ownerChange: Boolean,
        var renamed: Boolean,
        var activityChange: Boolean
    )

    enum class PermissionsInherited {
        GENERATOR, CATEGORY, NONE;
    }

    enum class InitialPosition {
        BEFORE, AFTER, BOTTOM;
    }

    /**
     * @param permissionDenied the permission that should be denied when this state is applied
     * @param permissionReset the permissions that should be reset when this state is applied
     */
    enum class VCState(
        val permissionDenied: Permission?,
        val permissionReset: Permission?
    ) {
        UNLOCKED(null, Permission.VOICE_CONNECT),
        LOCKED(Permission.VOICE_CONNECT, null),
        HIDDEN(Permission.VIEW_CHANNEL, null),
        UNHIDDEN(null, Permission.VIEW_CHANNEL);
    }
}
