package space.astro.api.central.utils

import net.dv8tion.jda.api.Permission

/**
 * Stores logical information about a Discord permission
 *
 * @param id
 * @param name
 * @param channelTypes Supported channel types on which the permission can be applied
 * @param category permissions grouping as appears in a Discord channel settings
 */
data class DiscordPermissionMeta(
    val id: String,
    val name: String,
    val channelTypes: List<DiscordPermissionMetaChannelType>,
    val category: DiscordPermissionMetaCategory
) {
    enum class DiscordPermissionMetaChannelType {
        TEXT,
        VOICE,
        STAGE
    }

    enum class DiscordPermissionMetaCategory {
        GENERAL,
        MEMBERSHIP,
        APPS,
        TEXT,
        VOICE
    }

    companion object {
        val channelsRelatedPermissions = Permission.entries.mapNotNull {
            when (it) {
                Permission.MANAGE_SERVER,
                Permission.VIEW_AUDIT_LOGS,
                Permission.VIEW_GUILD_INSIGHTS,
                Permission.MANAGE_ROLES,
                Permission.MANAGE_GUILD_EXPRESSIONS,
                Permission.MANAGE_EVENTS,
                Permission.VIEW_CREATOR_MONETIZATION_ANALYTICS,
                Permission.CREATE_GUILD_EXPRESSIONS,
                Permission.CREATE_SCHEDULED_EVENTS,
                Permission.KICK_MEMBERS,
                Permission.BAN_MEMBERS,
                Permission.NICKNAME_CHANGE,
                Permission.NICKNAME_MANAGE,
                Permission.MODERATE_MEMBERS,
                Permission.ADMINISTRATOR,
                Permission.UNKNOWN -> null

                Permission.MANAGE_CHANNEL -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(
                        DiscordPermissionMetaChannelType.TEXT,
                        DiscordPermissionMetaChannelType.VOICE,
                        DiscordPermissionMetaChannelType.STAGE
                    ),
                    DiscordPermissionMetaCategory.GENERAL
                )

                Permission.VIEW_CHANNEL -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(
                        DiscordPermissionMetaChannelType.TEXT,
                        DiscordPermissionMetaChannelType.VOICE,
                        DiscordPermissionMetaChannelType.STAGE
                    ),
                    DiscordPermissionMetaCategory.GENERAL
                )

                Permission.MANAGE_PERMISSIONS -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(
                        DiscordPermissionMetaChannelType.TEXT,
                        DiscordPermissionMetaChannelType.VOICE,
                        DiscordPermissionMetaChannelType.STAGE
                    ),
                    DiscordPermissionMetaCategory.GENERAL
                )

                Permission.MANAGE_WEBHOOKS -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(
                        DiscordPermissionMetaChannelType.TEXT,
                        DiscordPermissionMetaChannelType.VOICE,
                        DiscordPermissionMetaChannelType.STAGE
                    ),
                    DiscordPermissionMetaCategory.GENERAL
                )

                Permission.USE_EMBEDDED_ACTIVITIES -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(
                        DiscordPermissionMetaChannelType.TEXT,
                        DiscordPermissionMetaChannelType.VOICE,
                        DiscordPermissionMetaChannelType.STAGE
                    ),
                    DiscordPermissionMetaCategory.APPS
                )

                Permission.CREATE_INSTANT_INVITE -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(
                        DiscordPermissionMetaChannelType.TEXT,
                        DiscordPermissionMetaChannelType.VOICE,
                        DiscordPermissionMetaChannelType.STAGE
                    ),
                    DiscordPermissionMetaCategory.MEMBERSHIP
                )

                Permission.MESSAGE_ADD_REACTION,
                Permission.MESSAGE_SEND,
                Permission.MESSAGE_TTS,
                Permission.MESSAGE_MANAGE,
                Permission.MESSAGE_EMBED_LINKS,
                Permission.MESSAGE_ATTACH_FILES,
                Permission.MESSAGE_HISTORY,
                Permission.MESSAGE_MENTION_EVERYONE,
                Permission.MESSAGE_EXT_EMOJI,
                Permission.MESSAGE_EXT_STICKER,
                Permission.MESSAGE_ATTACH_VOICE_MESSAGE,
                Permission.MESSAGE_SEND_POLLS,
                Permission.MANAGE_THREADS,
                Permission.CREATE_PUBLIC_THREADS,
                Permission.CREATE_PRIVATE_THREADS,
                Permission.MESSAGE_SEND_IN_THREADS -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(
                        DiscordPermissionMetaChannelType.TEXT,
                        DiscordPermissionMetaChannelType.VOICE,
                        DiscordPermissionMetaChannelType.STAGE
                    ),
                    DiscordPermissionMetaCategory.TEXT
                )

                Permission.USE_APPLICATION_COMMANDS,
                Permission.USE_EXTERNAL_APPLICATIONS -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(
                        DiscordPermissionMetaChannelType.TEXT,
                        DiscordPermissionMetaChannelType.VOICE,
                        DiscordPermissionMetaChannelType.STAGE
                    ),
                    DiscordPermissionMetaCategory.APPS
                )

                Permission.PRIORITY_SPEAKER,
                Permission.VOICE_CONNECT,
                Permission.VOICE_MUTE_OTHERS,
                Permission.VOICE_MOVE_OTHERS -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(
                        DiscordPermissionMetaChannelType.VOICE,
                        DiscordPermissionMetaChannelType.STAGE
                    ),
                    DiscordPermissionMetaCategory.VOICE
                )

                Permission.VOICE_STREAM,
                Permission.VOICE_SPEAK,
                Permission.VOICE_DEAF_OTHERS,
                Permission.VOICE_USE_VAD,
                Permission.VOICE_USE_SOUNDBOARD,
                Permission.VOICE_USE_EXTERNAL_SOUNDS,
                Permission.VOICE_SET_STATUS,
                Permission.REQUEST_TO_SPEAK -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(DiscordPermissionMetaChannelType.VOICE),
                    DiscordPermissionMetaCategory.VOICE
                )

                Permission.PIN_MESSAGES -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(DiscordPermissionMetaChannelType.TEXT),
                    DiscordPermissionMetaCategory.TEXT
                )

                Permission.BYPASS_SLOWMODE -> DiscordPermissionMeta(
                    it.name,
                    it.getName(),
                    listOf(
                        DiscordPermissionMetaChannelType.TEXT,
                        DiscordPermissionMetaChannelType.VOICE,
                        DiscordPermissionMetaChannelType.STAGE
                    ),
                    DiscordPermissionMetaCategory.TEXT
                )
            }
        }
    }
}