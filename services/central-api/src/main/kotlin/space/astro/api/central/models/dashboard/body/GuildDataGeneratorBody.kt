package space.astro.api.central.models.dashboard.body

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import space.astro.api.central.controllers.dashboard.DashboardDiscordPermissionsController
import space.astro.shared.core.models.database.*
import space.astro.shared.core.util.extention.asValidationResult
import space.astro.shared.core.util.extention.isValidSnowflake
import space.astro.shared.core.util.ui.Links
import space.astro.shared.core.util.validation.ValidationResult

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeneratorDataBody(
    val id: String,
    var fallbackId: String? = null,
    var queueMode: Boolean = false,
    var defaultName: String = "{nickname}'s VC",
    var defaultLockedName: String? = "Locked | {nickname}'s VC",
    var defaultHiddenName: String? = "Hidden | {nickname}'s VC",
    var userLimit: Int = 0,
    var bitrate: Int = 0,
    var category: String? = null,
    var permissionsInherited: PermissionsInherited = PermissionsInherited.GENERATOR,
    var permissionsTargetRole: String? = null,
    var permissionsImmuneRole: String? = null,
    // list of permissions IDs
    var ownerPermissions: List<String> = emptyList(),
    var ownerRole: String? = null,
    var initialState: VCState = VCState.UNLOCKED,
    var initialPosition: InitialPosition = InitialPosition.BOTTOM,
    var renameConditions: RenameConditions = RenameConditions(),
    var commandsSettings: CommandsSettings = CommandsSettings(),

    var autoChat: Boolean = false,
    var autoWaiting: Boolean = false,

    var chatCategory: String? = category,
    var chatTopic: String? = "Temporary text chat made by Astro | ${Links.WEBSITE}",
    var chatNsfw: Boolean = false,
    var chatSlowmode: Int = 0,
    var chatPermissionsInherited: PermissionsInherited = PermissionsInherited.NONE,
    var defaultChatName: String = "{vc_name}",
    var defaultChatText: String? = null,
    var defaultChatTextEmbed: Boolean = true,
    var chatInterface: Int = -1,

    var waitingCategory: String? = category,
    var waitingPermissionsInherited: PermissionsInherited = PermissionsInherited.NONE,
    var defaultWaitingName: String = "Waiting for {vc_name}",
    var waitingBitrate: Int = 0,
    var waitingPosition: InitialPosition = InitialPosition.BEFORE,
    var waitingUserLimit: Int = 0,
) {
    fun toGeneratorData() = GeneratorData(
        id = id,
        fallbackId = fallbackId,
        queueMode = queueMode,
        defaultName = defaultName,
        defaultLockedName = defaultLockedName,
        defaultHiddenName = defaultHiddenName,
        userLimit = userLimit,
        bitrate = bitrate,
        category = category,
        permissionsInherited = permissionsInherited,
        permissionsTargetRole = permissionsTargetRole,
        permissionsImmuneRole = permissionsImmuneRole,
        ownerPermissions = Permission.getRaw(ownerPermissions.mapNotNull { try { Permission.valueOf(it) } catch(e: Exception) { null } }),
        ownerRole = ownerRole,
        initialState = initialState,
        initialPosition = initialPosition,
        renameConditions = renameConditions,
        commandsSettings = commandsSettings,
        autoChat = autoChat,
        autoWaiting = autoWaiting,
        chatCategory = chatCategory,
        chatTopic = chatTopic,
        chatNsfw = chatNsfw,
        chatSlowmode = chatSlowmode,
        chatPermissionsInherited = chatPermissionsInherited,
        defaultChatName = defaultChatName,
        defaultChatText = defaultChatText,
        defaultChatTextEmbed = defaultChatTextEmbed,
        chatInterface = chatInterface,
        waitingCategory = waitingCategory,
        waitingPermissionsInherited = waitingPermissionsInherited,
        defaultWaitingName = defaultWaitingName,
        waitingBitrate = waitingBitrate,
        waitingPosition = waitingPosition,
        waitingUserLimit = waitingUserLimit
    )
}