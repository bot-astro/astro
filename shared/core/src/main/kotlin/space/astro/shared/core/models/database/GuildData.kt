package space.astro.shared.core.models.database

import com.aventrix.jnanoid.jnanoid.NanoIdUtils
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.Region
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.Widget
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import space.astro.shared.core.util.extention.*
import space.astro.shared.core.util.ui.Colors
import space.astro.shared.core.util.ui.Links
import space.astro.shared.core.util.validation.ValidationResult

data class GuildData(
    val guildID: String,
    var upgradedByUserID: String? = null,
    val entitlements: MutableList<GuildEntitlement> = mutableListOf(),
    var bannedCommands: MutableList<String> = mutableListOf(),
    val templates: MutableList<TemplateData> = mutableListOf(),
    val connections: MutableList<ConnectionData> = mutableListOf(),
    val generators: MutableList<GeneratorData> = mutableListOf(),
    var interfaces: MutableList<InterfaceData> = mutableListOf(),
    var errorLogsChannelId: String? = null,
    var allowMissingAdminPerm: Boolean = false,
)

/**
 * Entitlement owned by a guild
 *
 * @param id Id of the entitlement
 * @param skuId Id of the SKU of the entitlement
 * @param endsAt Expiration of the entitlement in epoch millis, NULL if it never expires
 */
data class GuildEntitlement(
    val id: String,
    val skuId: String,
    val endsAt: Long?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TemplateData(
    val id: String = NanoIdUtils.randomNanoId(),
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class GeneratorData(
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
    var ownerPermissions: Long = 0,
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
    var waitingUserLimit: Int = 0
) {
    fun validate() : ValidationResult {
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
}


enum class PermissionsInherited {
    GENERATOR, CATEGORY, NONE;

    override fun toString(): String {
        return when (this) {
            GENERATOR -> "From the generator"
            CATEGORY -> "From the category (in which they get generated)"
            NONE -> "Do not inherit any permission"
        }
    }
}

enum class InitialPosition {
    BEFORE, AFTER, BOTTOM;

    override fun toString(): String {
        return when (this) {
            BEFORE -> "Before the generator"
            AFTER -> "After the generator"
            BOTTOM -> "Bottom of the category"
        }
    }
}

/**
 * @param permissionDenied the permission that should be denied when this state is applied
 * @param permissionReset the permissions that should be reset when this state is applied
 */
enum class VCState(
    val permissionDenied: Permission? = null,
    val permissionReset: Permission? = null
) {
    UNLOCKED(null, Permission.VOICE_CONNECT),
    LOCKED(Permission.VOICE_CONNECT),
    HIDDEN(Permission.VIEW_CHANNEL),
    UNHIDDEN(null, Permission.VIEW_CHANNEL);
}


@JsonIgnoreProperties(ignoreUnknown = true)
data class InterfaceData(
    var channelID: String,
    var messageID: String,
    var buttons: MutableList<InterfaceButton> = mutableListOf(),

    @Deprecated("Use buttons property instead")
    var actions: MutableList<InterfaceAction> = mutableListOf(),

    val embedStyle: EmbedStyle = EmbedStyle(),
    var generateEmbedFields: Boolean = false
) {
    fun asMarkdownLink(guildID: String) = "interface".asMessageMarkdownLink(guildID, channelID, messageID)

    fun validate() : ValidationResult {
        val channelIdValidation = channelID.isValidSnowflake().asValidationResult("invalid channel id")
        val messageIdValidation = messageID.isValidSnowflake().asValidationResult("invalid message id")
        val buttonsValidation = buttons.map { it.validate() }.firstOrNull { !it.isValid } ?: ValidationResult.valid()
        val embedStyleValidation = embedStyle.validate()

        return ValidationResult.combine(
            channelIdValidation,
            messageIdValidation,
            buttonsValidation,
            embedStyleValidation
        )
    }
}

data class EmbedStyle(
    var url: String? = Links.WEBSITE,
    var title: String? = null,
    var description: String? = "You can use this interface to manage your voice channel.\nYou can also use `/vc` slash commands!",
    var timestamp: Long? = null,
    var color: Int = Colors.purple.rgb,
    var thumbnail: String? = null,
    var image: String? = Links.INTERFACE_BUTTONS_IMAGE,
    var authorName: String? = "Astro Interface",
    var authorUrl: String? = Links.WEBSITE,
    var authorIconUrl: String? = Links.LOGO,
    var footer: String? = "Use the buttons below to manage your voice channel",
    var footerIconUrl: String? = null
) {
    fun validate() : ValidationResult {
        val urlValidation = (url?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the url for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")
        val titleValidation = (title?.length?.let { it < MessageEmbed.TITLE_MAX_LENGTH } ?: true).asValidationResult("the title for the embed must be less than ${MessageEmbed.TITLE_MAX_LENGTH} characters")
        val descriptionValidation = (description?.length?.let { it < MessageEmbed.DESCRIPTION_MAX_LENGTH } ?: true).asValidationResult("the description for the embed must be less than ${MessageEmbed.DESCRIPTION_MAX_LENGTH} characters")
        val thumbnailValidation = (thumbnail?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the thumbnail for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")
        val imageValidation = (image?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the image for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")
        val authorNameValidation = (authorName?.length?.let { it < MessageEmbed.AUTHOR_MAX_LENGTH } ?: true).asValidationResult("the author name for the embed must be less than ${MessageEmbed.AUTHOR_MAX_LENGTH} characters")
        val authorUrlValidation = (authorUrl?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the author url for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")
        val authorIconUrlValidation = (authorIconUrl?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the author icon url for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")
        val footerValidation = (footer?.length?.let { it < MessageEmbed.TEXT_MAX_LENGTH } ?: true).asValidationResult("the footer for the embed must be less than ${MessageEmbed.TEXT_MAX_LENGTH} characters")
        val footerIconUrlValidation = (footerIconUrl?.length?.let { it < MessageEmbed.URL_MAX_LENGTH } ?: true).asValidationResult("the footer icon url for the embed must be less than ${MessageEmbed.URL_MAX_LENGTH} characters")

        return ValidationResult.combine(
            urlValidation,
            titleValidation,
            descriptionValidation,
            thumbnailValidation,
            imageValidation,
            authorNameValidation,
            authorUrlValidation,
            authorIconUrlValidation,
            footerValidation,
            footerIconUrlValidation
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class InterfaceAction(
    /**
     * Action path
     */
    var cmdPath: String,
    var name: String? = null,
    var emoji: String? = null,
    var buttonStyleKey: Int = ButtonStyle.SECONDARY.key,
    var buttonDisabled: Boolean = false,
    var position: Pair<Int, Int>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InterfaceButton(
    var id: String,
    var name: String? = null,
    var emoji: String? = null,
    var buttonStyleKey: Int = ButtonStyle.SECONDARY.key,
    var buttonDisabled: Boolean = false,
    var position: Pair<Int, Int>,
    var fieldValue: String = id
) {
    fun validate() : ValidationResult {
        val firstPositionValidation = (position.first in 0..4).asValidationResult("invalid button starting position, must be between 0 and 4")
        val secondPositionValidation = (position.second in 0..4).asValidationResult("invalid button ending position, must be between 0 and 4")

        return ValidationResult.combine(
            firstPositionValidation,
            secondPositionValidation
        )
    }

    companion object {
        fun fromButton(button: Button, position: Pair<Int, Int>) = InterfaceButton(
            id = button.id!!.plus("?interface=true"),
            name = button.label,
            emoji = button.emoji?.formatted,
            buttonStyleKey = button.style.key,
            false,
            position = position,
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class ConnectionData(
    var id: String,
    var roleID: String,
    var action: ConnectionAction = ConnectionAction.ASSIGN
) {
    fun validate() : ValidationResult {
        val idValidation = id.isValidSnowflake().asValidationResult("invalid channel id")
        val roleIDValidation = roleID.isValidSnowflake().asValidationResult("invalid role id")

        return ValidationResult.combine(
            idValidation,
            roleIDValidation
        )
    }

    override fun toString(): String {
        val joinActionName = when (action) {
            ConnectionAction.ASSIGN -> "receive"
            ConnectionAction.REMOVE -> "loose"
            ConnectionAction.TOGGLE -> "get toggled"
        }
        val leaveActionName = when (action) {
            ConnectionAction.ASSIGN -> "removed"
            ConnectionAction.REMOVE -> "back"
            ConnectionAction.TOGGLE -> "toggled back"
        }

        return "Users will $joinActionName the ${roleID.asRoleMention()} role when joining ${id.asChannelMention()}" +
            " and they will ${if (action.permanent) "__not__ " else ""}get that role $leaveActionName" +
            " when they leave the channel." +
            "\n" +
            "\n**Summary**" +
            "\n> **Channel** > ${id.asChannelMention()}" +
            "\n> **Role** > ${roleID.asRoleMention()}" +
            "\n> **Action** > ${action.name.lowercaseAndCapitalize()}" +
            "\n> **Permanent** > ${action.permanent.asTrueOrFalse()}"
    }
}

enum class ConnectionAction(
    var permanent: Boolean = false
) {
    ASSIGN, REMOVE, TOGGLE;

    override fun toString(): String = when (this) {
        ASSIGN -> "Assign the role"
        REMOVE -> "Remove the role"
        TOGGLE -> "Toggle the role (assign if missing, remove if present)"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommandsSettings(
    var maxUserLimit: Int = 99,
    var minUserLimit: Int = 0,

    var maxBitrate: Int? = null,
    var minBitrate: Int = 8000,

    var badwordsAllowed: Boolean = true,
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

@JsonIgnoreProperties(ignoreUnknown = true)
data class RenameConditions(
    var stateChange: Boolean = true,
    var ownerChange: Boolean = true,
    var renamed: Boolean = false,
    var activityChange: Boolean = true
)