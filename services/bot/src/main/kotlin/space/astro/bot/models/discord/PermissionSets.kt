package space.astro.bot.models.discord

import net.dv8tion.jda.api.Permission

object PermissionSets {
    val astroSendMessagePermissions = Permission.getRaw(
        Permission.MESSAGE_SEND,
        Permission.MESSAGE_EMBED_LINKS
    )

    val astroVCPermissions = Permission.getRaw(
        Permission.VIEW_CHANNEL,
        Permission.MANAGE_CHANNEL,
        Permission.VOICE_CONNECT,
        Permission.VOICE_SPEAK,
        Permission.CREATE_INSTANT_INVITE,
        Permission.MESSAGE_SEND,
        Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_EXT_EMOJI,
    )

    val astroPrivateChatPermissions = Permission.getRaw(
        Permission.VIEW_CHANNEL,
        Permission.MANAGE_CHANNEL,
        Permission.MESSAGE_SEND,
        Permission.MESSAGE_EMBED_LINKS,
        Permission.MESSAGE_EXT_EMOJI,
        Permission.MESSAGE_HISTORY,
        Permission.MESSAGE_ATTACH_FILES
    )

    val immuneRoleVCPermissions = Permission.getRaw(
        Permission.VIEW_CHANNEL,
        Permission.VOICE_CONNECT,
        Permission.VOICE_USE_VAD,
        Permission.VOICE_SPEAK
    )

    val immuneRolePrivateChatPermissions = Permission.getRaw(
        Permission.VIEW_CHANNEL
    )

    val ownerVCPermissions = Permission.getRaw(
        Permission.VOICE_MOVE_OTHERS,
        Permission.VOICE_CONNECT,
        Permission.VIEW_CHANNEL
    )

    val ownerWaitingRoomVCPermissions = Permission.getRaw(
        Permission.VOICE_MOVE_OTHERS,
        Permission.VOICE_CONNECT,
        Permission.VIEW_CHANNEL
    )

    val userTemporaryVCPermissions = Permission.getRaw(
        Permission.VIEW_CHANNEL,
        Permission.VOICE_CONNECT
    )

    val userTemporaryVCChatPermissions = Permission.getRaw(
        Permission.VIEW_CHANNEL,
        Permission.MESSAGE_SEND
    )
}