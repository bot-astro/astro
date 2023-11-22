package space.astro.bot.listeners.member

import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.events.user.update.UserUpdateActivitiesEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.exceptions.ConfigurationException
import space.astro.bot.managers.cooldown.CooldownsManager
import space.astro.bot.managers.util.PremiumRequirementDetector
import space.astro.bot.managers.vc.VCNameManager
import space.astro.bot.managers.vc.VCPrivateChatManager
import space.astro.bot.managers.vc.VCWaitingRoomManager
import space.astro.bot.managers.vc.ctx.VCOperationCTX
import space.astro.shared.core.services.dao.GuildDao
import space.astro.shared.core.services.dao.TemporaryVCDao

@Component
class MemberUpdateActivitiesEventListener(
    val cooldownsManager: CooldownsManager,
    val temporaryVCDao: TemporaryVCDao,
    val guildDao: GuildDao,
    val premiumRequirementDetector: PremiumRequirementDetector,
    val vcNameManager: VCNameManager,
    val vcPrivateChatManager: VCPrivateChatManager,
    val vcWaitingRoomManager: VCWaitingRoomManager
) {

    @EventListener
    fun receiveMemberUpdateActivitiesEvent(event: UserUpdateActivitiesEvent) {
        if (event.user.isBot)
            return

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
            // TODO: Send error
        }

        vcOperationCTX.queueUpdatedManagers { _, _ ->
            // TODO: Error
        }

        temporaryVCDao.save(guildId, vcOperationCTX.temporaryVCData)
    }
}