package space.astro.bot.events.listeners.voice.handlers

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.Permission
import space.astro.bot.core.extentions.modifyPermissionOverride
import space.astro.bot.core.ui.Emojis
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEvent

fun VCEventHandler.handleJoinedTemporaryVCEvent(
    event: VCEvent.JoinedTemporaryVC,
    memberRolesManager: SimpleMemberRolesManager,
) {
    val data = event.vcEventData
    val guild = data.guild
    val member = data.member

    ///////////////////
    /// STATE CHECK ///
    ///////////////////

    if (data.joinedChannel == null) {
        throw IllegalStateException("Received joined temporary VC event with a null joined channel")
    }

    val temporaryVCData = event.temporaryVCData
    val temporaryVC = data.joinedChannel.asVoiceChannel()

    //////////////////////////
    /// PERMISSION UPDATES ///
    //////////////////////////

    // required for letting the user use the voice channel fully even when locked or hidden
    // see https://support.discord.com/hc/en-us/community/posts/16947316767127-Video-and-Send-messages-permissions-suppressed-by-Connect
    temporaryVC.manager.modifyPermissionOverride(
        permissionHolder = member,
        allow = PermissionSets.userTemporaryVCPermissions
    ).queue()

    // allow user to see the private text chat of the temporary VC if existing
    val privateChat = temporaryVCData.chatID
        ?.let { guild.getTextChannelById(it) }
        ?.also {
            it.upsertPermissionOverride(member)
                .grant(PermissionSets.userTemporaryVCChatPermissions)
                .queue()
        }

    ////////////////
    /// LOG CHAT ///
    ////////////////
    val logChat = privateChat ?: temporaryVC

    if (temporaryVCData.chatLogs && guild.selfMember.hasPermission(logChat, Permission.getPermissions(PermissionSets.astroSendMessagePermissions))) {
        val userJoinedEmbed = Embed {
            color = guild.selfMember.colorRaw
            title = "${Emojis.logs.formatted} Voice channel logs"
            description = "*${data.member.asMention} just joined the VC!*"
            footer {
                name = "You can disable these logs with `/chat logs`"
            }
        }

        logChat.sendMessageEmbeds(userJoinedEmbed).queue()
    }
}