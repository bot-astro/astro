package space.astro.bot.events.listeners.voice.handlers

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.Permission
import space.astro.bot.core.ui.Emojis
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.models.discord.vc.event.VCEvent

fun VCEventHandler.handleLeftTemporaryVCEvent(
    event: VCEvent.LeftTemporaryVC,
    memberRolesManager: SimpleMemberRolesManager,
) {
    val data = event.vcEventData
    val guild = data.guild
    val vc = data.leftChannel?.asVoiceChannel()
        ?: throw IllegalStateException("Received left temporary VC event with a null left channel")
    val privateChat = event.temporaryVCData.chatID?.let { guild.getTextChannelById(it) }
    val waitingRoom = event.temporaryVCData.waitingID?.let { guild.getVoiceChannelById(it) }
    val newOwner = vc.members.firstOrNull { !it.user.isBot }

    ////////////////
    /// EMPTY VC ///
    ////////////////
    if (newOwner == null) {
        temporaryVCDao.delete(guild.id, event.temporaryVCData.id)

        vc.delete().queue()
        privateChat?.delete()?.queue()
        waitingRoom?.delete()?.queue()

        return
    }

    if (event.ownerLeft) {
        //////////////////
        /// OWNER LEFT ///
        //////////////////
        val generator = guild.getVoiceChannelById(event.temporaryVCData.generatorId)
        val generatorData = data.generators.firstOrNull { it.id == event.temporaryVCData.generatorId }

        if (generatorData != null && generator != null) {
            val vcOperationCTX = VCOperationCTX(
                guildData = event.vcEventData.guildData,
                generator = generator,
                generatorData = generatorData,
                temporaryVCOwner = data.member,
                temporaryVC = vc,
                temporaryVCManager = vc.manager,
                temporaryVCData = event.temporaryVCData,
                temporaryVCsData = event.vcEventData.temporaryVCs,
                privateChat = privateChat,
                privateChatManager = privateChat?.manager,
                waitingRoom = waitingRoom,
                waitingRoomManager = waitingRoom?.manager,
                vcOperationOrigin = VCOperationCTX.VCOperationOrigin.OWNER_CHANGE
            )

            // Owner role is handled here because it needs the memberRolesManager
            val ownerRole = generatorData.ownerRole?.let { guild.getRoleById(it) }
            ownerRole?.also { memberRolesManager.remove(it) }

            vcOwnershipManager.changeOwner(vcOperationCTX, newOwner)

            vcOperationCTX.queueUpdatedManagers(
                failure = { managerType, e ->
                    configurationErrorEventPublisher.publishConfigurationErrorEvent(
                        guildId = guild.id,
                        configurationErrorData = configurationErrorService.unknownError(
                            encounteredIn = "updating ${managerType.readableName}: ${e.message ?: ""}"
                        )
                    )
                }
            )
            temporaryVCDao.save(guild.id, vcOperationCTX.temporaryVCData)

            ownerRole?.also { guild.addRoleToMember(newOwner, it).queue() }
        }
    } else {
        //////////////////////
        /// NON-OWNER LEFT ///
        //////////////////////
        vc.manager.removePermissionOverride(data.member.idLong).queue()
        privateChat?.manager?.removePermissionOverride(data.member.idLong)?.queue()
    }

    
    ////////////////
    /// LOG CHAT ///
    ////////////////
    val logChat = privateChat ?: vc

    if (event.temporaryVCData.chatLogs && guild.selfMember.hasPermission(logChat, Permission.getPermissions(
            PermissionSets.astroSendMessagePermissions))) {
        val userLeftEmbed = Embed {
            color = guild.selfMember.colorRaw
            title = "${Emojis.logs.formatted} Voice channel logs"
            description = "*${data.member.asMention} just left the VC.*"
            footer {
                name = "You can disable these logs with `/chat logs`"
            }
        }

        logChat.sendMessageEmbeds(userLeftEmbed).queue()
    }
}