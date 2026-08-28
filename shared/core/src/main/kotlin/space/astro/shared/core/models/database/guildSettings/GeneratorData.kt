package space.astro.shared.core.models.database.guildSettings

data class GeneratorData(
    val id: String,
//    val id: String,
//    var fallbackId: String? = null,
//    var queueMode: Boolean = false,
//    var defaultName: String = "{nickname}'s VC",
//    var defaultLockedName: String? = "Locked | {nickname}'s VC",
//    var defaultHiddenName: String? = "Hidden | {nickname}'s VC",
//    var userLimit: Int = 0,
//    var bitrate: Int = 0,
//    var category: String? = null,
//    var permissionsInherited: PermissionsInherited = PermissionsInherited.GENERATOR,
//    var permissionsTargetRole: String? = null,
//    var permissionsImmuneRole: String? = null,
//    var ownerPermissions: Long = 0,
//    var ownerRole: String? = null,
//    var initialState: VCState = VCState.UNLOCKED,
//    var initialPosition: InitialPosition = InitialPosition.BOTTOM,
//    var renameConditions: RenameConditions = RenameConditions(),
//    var commandsSettings: CommandsSettings = CommandsSettings(),
//
//    var autoChat: Boolean = false,
//    var autoWaiting: Boolean = false,
//
//    var chatCategory: String? = category,
//    var chatTopic: String? = "Temporary text chat made by Astro | ${Links.WEBSITE}",
//    var chatNsfw: Boolean = false,
//    var chatSlowmode: Int = 0,
//    var chatPermissionsInherited: PermissionsInherited = PermissionsInherited.NONE,
//    var defaultChatName: String = "{vc_name}",
//    var defaultChatText: String? = null,
//    var defaultChatTextEmbed: Boolean = true,
//    var chatInterface: Int = -1,
//
//    var waitingCategory: String? = category,
//    var waitingPermissionsInherited: PermissionsInherited = PermissionsInherited.NONE,
//    var defaultWaitingName: String = "Waiting for {vc_name}",
//    var waitingBitrate: Int = 0,
//    var waitingPosition: InitialPosition = InitialPosition.BEFORE,
//    var waitingUserLimit: Int = 0,
//
//    var ownerPermissionIds: List<String> = Permission.getPermissions(ownerPermissions).map { it.name }
) {
//    fun parseAndValidate() : ValidationResult {
//        ownerPermissions = Permission.getRaw(ownerPermissionIds.mapNotNull { try { Permission.valueOf(it) } catch(e: Exception) { null } })
//
//        val idValidation = id.isValidSnowflake().asValidationResult("invalid generator id")
//        val fallbackIdValidation = (fallbackId?.isValidSnowflake() ?: true).asValidationResult("invalid fallback generator id")
//        val defaultNameValidation = (defaultName.length in 2..500).asValidationResult("the default name for the generator must be between 2 and 500 characters")
//        val defaultLockedNameValidation = (defaultLockedName?.length?.let { it in 2..500 } ?: true).asValidationResult("the default locked name for the generator must be between 2 and 500 characters")
//        val defaultHiddenNameValidation = (defaultHiddenName?.length?.let { it in 2..500 } ?: true).asValidationResult("the default hidden name for the generator must be between 2 and 500 characters")
//        val userLimitValidation = (userLimit in 0..99).asValidationResult("the user limit for the generator must be between 0 and 99")
//        val bitrateValidation = (bitrate in 0..384000).asValidationResult("the bitrate for the generator must be between 0 and 384000")
//        val categoryValidation = (category?.isValidSnowflake() ?: true).asValidationResult("invalid category id")
//        val permissionsTargetRoleValidation = (permissionsTargetRole?.isValidSnowflake() ?: true).asValidationResult("invalid permissions target role id")
//        val permissionsImmuneRoleValidation = (permissionsImmuneRole?.isValidSnowflake() ?: true).asValidationResult("invalid permissions immune role id")
//        val ownerRoleValidation = (ownerRole?.isValidSnowflake() ?: true).asValidationResult("invalid owner role id")
//        val commandsSettingsValidation = commandsSettings.validate()
//        val chatCategoryValidation = (chatCategory?.isValidSnowflake() ?: true).asValidationResult("invalid chat category id")
//        val chatTopicValidation = (chatTopic?.length?.let { it in 0.. TextChannel.MAX_TOPIC_LENGTH } ?: true).asValidationResult("the chat topic for the generator must be between 0 and ${TextChannel.MAX_TOPIC_LENGTH} characters")
//        val chatSlowmodeValidation = (chatSlowmode in 0..TextChannel.MAX_SLOWMODE).asValidationResult("the chat slowmode for the generator must be between 0 and ${TextChannel.MAX_SLOWMODE}")
//        val defaultChatNameValidation = (defaultChatName.length in 2..500).asValidationResult("the default chat name for the generator must be between 2 and 500 characters")
//        val maxDefaultChatTextLength = if (defaultChatTextEmbed) MessageEmbed.DESCRIPTION_MAX_LENGTH else MessageEmbed.TEXT_MAX_LENGTH
//        val defaultChatTextValidation = (defaultChatText?.length?.let { it in 0..maxDefaultChatTextLength } ?: true).asValidationResult("the default chat text for the generator must be between 0 and $maxDefaultChatTextLength characters")
//        val waitingCategoryValidation = (waitingCategory?.isValidSnowflake() ?: true).asValidationResult("invalid waiting room category id")
//        val defaultWaitingNameValidation = (defaultWaitingName.length in 2..500).asValidationResult("the default waiting room name for the generator must be between 2 and 500 characters")
//        val waitingBitrateValidation = (waitingBitrate in 0..384000).asValidationResult("the waiting room bitrate for the generator must be between 0 and 384000")
//        val waitingUserLimitValidation = (waitingUserLimit in 0..99).asValidationResult("the waiting room user limit for the generator must be between 0 and 99")
//
//        return ValidationResult.combine(
//            idValidation,
//            fallbackIdValidation,
//            defaultNameValidation,
//            defaultLockedNameValidation,
//            defaultHiddenNameValidation,
//            userLimitValidation,
//            bitrateValidation,
//            categoryValidation,
//            permissionsTargetRoleValidation,
//            permissionsImmuneRoleValidation,
//            ownerRoleValidation,
//            commandsSettingsValidation,
//            chatCategoryValidation,
//            chatTopicValidation,
//            chatSlowmodeValidation,
//            defaultChatNameValidation,
//            defaultChatTextValidation,
//            waitingCategoryValidation,
//            defaultWaitingNameValidation,
//            waitingBitrateValidation,
//            waitingUserLimitValidation
//        )
//    }
}

enum class PermissionsInherited {
    GENERATOR, CATEGORY, NONE;
}

enum class InitialPosition {
    BEFORE, AFTER, BOTTOM;
}