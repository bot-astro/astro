package space.astro.bot.listeners.voice.handlers

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.Permission
import space.astro.bot.extentions.modifyPermissionOverride
import space.astro.bot.managers.roles.SimpleMemberRolesManager
import space.astro.bot.managers.util.PermissionSets
import space.astro.bot.managers.vc.events.VCEvent
import space.astro.bot.ui.Emojis

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
    ).queue({}) {
        TODO("Do we need to handle permission issues here or globally?")
    }

    // allow user to see the private text chat of the temporary VC if existing
    val privateChat = temporaryVCData.chatID
        ?.let { guild.getTextChannelById(it) }
        ?.also {
            if (premiumRequirementDetector.isGuildPremium(data.guildData)) {
                it.upsertPermissionOverride(member)
                    .grant(PermissionSets.userTemporaryVCChatPermissions)
                    .queue({}) {
                        TODO()
                    }
            } else {
                TODO()
            }
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
            /* TODO
            footer {
                name = "You can disable these logs with /${VcLogsSC().path}"
            }
             */
        }

        logChat.sendMessageEmbeds(userJoinedEmbed).queue()
    }
}