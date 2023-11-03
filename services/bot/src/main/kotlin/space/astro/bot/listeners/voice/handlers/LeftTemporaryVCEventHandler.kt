package space.astro.bot.listeners.voice.handlers

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.Permission
import space.astro.bot.managers.roles.SimpleMemberRolesManager
import space.astro.bot.managers.util.PermissionSets
import space.astro.bot.managers.vc.VCEvent
import space.astro.bot.managers.vc.VCManager
import space.astro.bot.ui.Emojis

fun VCEventHandler.handleLeftTemporaryVCEvent(
    event: VCEvent.LeftTemporaryVC,
    memberRolesManager: SimpleMemberRolesManager,
) {
    val data = event.vcEventData
    val guild = data.guild
    val vc = data.leftChannel?.asVoiceChannel()
        ?: throw IllegalStateException("Received left temporary VC event with a null left channel")
    val privateChat = event.temporaryVCData.chatID?.let { guild.getTextChannelById(it) }
    val newOwner = vc.members.firstOrNull { !it.user.isBot }

    ////////////////
    /// EMPTY VC ///
    ////////////////
    if (newOwner == null) {
        temporaryVCDao.delete(guild.id, event.temporaryVCData.id)

        vc.delete().queue()

        privateChat?.delete()?.queue()

        // TODO: Waiting rooms

        return
    }


    if (event.ownerLeft) {
        //////////////////
        /// OWNER LEFT ///
        //////////////////
        val generatorData = data.generators.firstOrNull { it.id == event.temporaryVCData.generatorId }

        if (generatorData != null) {
            val ownerRole = generatorData.ownerRole?.let { guild.getRoleById(it) }
            ownerRole?.also { memberRolesManager.remove(it) }

            // TODO: Change owner
            VCManager.changeOwner(guild)

            ownerRole?.also { guild.addRoleToMember(newOwner, it).queue() }
        }
    } else {
        //////////////////////
        /// NON-OWNER LEFT ///
        //////////////////////
        // TODO: Find a solution, don't like this
        vc.manager.removePermissionOverride(data.member.idLong).queue()
        privateChat?.manager?.removePermissionOverride(data.member.idLong)?.queue()
    }

    
    ////////////////
    /// LOG CHAT ///
    ////////////////
    val logChat = privateChat ?: vc

    if (event.temporaryVCData.chatLogs && guild.selfMember.hasPermission(logChat, Permission.getPermissions(PermissionSets.astroSendMessagePermissions))) {
        val userLeftEmbed = Embed {
            color = guild.selfMember.colorRaw
            title = "${Emojis.logs.formatted} Voice channel logs"
            description = "*${data.member.asMention} just left the VC.*"
            /* TODO
            footer {
                name = "You can disable these logs with /${VcLogsSC().path}"
            }
             */
        }

        logChat.sendMessageEmbeds(userLeftEmbed).queue()
    }

}