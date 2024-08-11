package space.astro.bot.events.listeners.voice.handlers

import dev.minn.jda.ktx.messages.Embed
import mu.KotlinLogging
import net.dv8tion.jda.api.Permission
import space.astro.bot.core.ui.Emojis
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.models.discord.vc.event.VCEvent

private val log = KotlinLogging.logger {  }

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

        log.info { "DELETE: Deleting temporary vc, detected ${vc.members.size} users, filtered to ${vc.members.filter { !it.user.isBot }.size} non bots - server ${guild.id}" }
        vc.delete()
            .reason("Deleting temporary vc, detected ${vc.members.size} users, filtered to ${vc.members.filter { !it.user.isBot }.size} non bots")
            .queue()
        privateChat?.delete()
            ?.reason("Deleting temporary vc, detected ${vc.members.size} users, filtered to ${vc.members.filter { !it.user.isBot }.size} non bots")
            ?.queue()
        waitingRoom?.delete()
            ?.reason("Deleting temporary vc, detected ${vc.members.size} users, filtered to ${vc.members.filter { !it.user.isBot }.size} non bots")
            ?.queue()

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
                        configurationErrorData = configurationErrorService.unknown(
                            guildId = guild.id,
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
        waitingRoom?.manager?.removePermissionOverride(data.member.idLong)?.queue()
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