package space.astro.bot.events.listeners.member

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.CooldownsManager
import space.astro.bot.components.managers.PremiumRequirementDetector
import space.astro.bot.components.managers.vc.VCNameManager
import space.astro.bot.components.managers.vc.VCPrivateChatManager
import space.astro.bot.components.managers.vc.VCWaitingRoomManager
import space.astro.bot.core.exceptions.ConfigurationException
import space.astro.bot.core.exceptions.VcOperationException
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.services.ConfigurationErrorService
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.TemporaryVCDao

@Component
class MemberUpdateActivitiesEventListener(
    val cooldownsManager: CooldownsManager,
    val temporaryVCDao: TemporaryVCDao,
    val guildDao: GuildDao,
    val premiumRequirementDetector: PremiumRequirementDetector,
    val vcNameManager: VCNameManager,
    val vcPrivateChatManager: VCPrivateChatManager,
    val vcWaitingRoomManager: VCWaitingRoomManager,
    val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    val configurationErrorService: ConfigurationErrorService
) {

    @EventListener
    fun receiveMemberUpdateActivitiesEvent(event: UserUpdateActivitiesEvent) {
        if (event.user.isBot)
            return

        // TODO: Global user cooldown check here?

        val guild = event.guild
        val guildId = guild.id
        val vc = event.member.voiceState!!
            .channel
            ?.takeIf { it.type == ChannelType.VOICE }
            ?.asVoiceChannel()
            ?: return

        val temporaryVCsData = temporaryVCDao.getAll(guildId)
        val temporaryVCData = temporaryVCsData.firstOrNull { it.id == vc.id }
            ?.takeIf { it.ownerId == event.member.id }
            ?: return

        val guildData = guildDao.get(guildId)
            ?.takeIf { premiumRequirementDetector.isGuildPremium(it) }
            ?: return

        val generatorData = guildData.generators
            .firstOrNull { it.id == temporaryVCData.generatorId }
            ?.takeIf {
                it.renameConditions.activityChange
                        || (!it.renameConditions.renamed && temporaryVCData.renamed)
            }
            ?: return

        val generator = guild.getVoiceChannelById(generatorData.id) ?: return
        val privateChat = temporaryVCData.chatID?.let { guild.getTextChannelById(it) }
        val waitingRoom = temporaryVCData.waitingID?.let { guild.getVoiceChannelById(it) }

        val vcOperationCTX = VCOperationCTX(
            guildData = guildData,
            generator = generator,
            generatorData = generatorData,
            temporaryVCOwner = event.member,
            temporaryVC = vc,
            temporaryVCManager = vc.manager,
            temporaryVCData = temporaryVCData,
            temporaryVCsData = temporaryVCsData,
            privateChat = privateChat,
            privateChatManager = privateChat?.manager,
            waitingRoom = waitingRoom,
            waitingRoomManager = waitingRoom?.manager,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.ACTIVITY_CHANGE
        )

        try {
            vcNameManager.performVCNameRefresh(vcOperationCTX)
            vcPrivateChatManager.performPrivateChatNameRefresh(vcOperationCTX)
            vcWaitingRoomManager.performWaitingRoomNameRefresh(vcOperationCTX)
        } catch (e: ConfigurationException) {
            configurationErrorEventPublisher.publishConfigurationErrorEvent(
                guildId = guildId,
                configurationErrorDto = e.configurationErrorDto
            )
        } catch (_: VcOperationException) {}

        vcOperationCTX.queueUpdatedManagers { managerType, e ->
            configurationErrorEventPublisher.publishConfigurationErrorEvent(
                guildId = guildId,
                configurationErrorDto = configurationErrorService.unknownError(
                    encounteredIn = "updating ${managerType.readableName} name: ${e.message ?: ""}"
                )
            )
        }

        temporaryVCDao.save(guildId, vcOperationCTX.temporaryVCData)
    }
}