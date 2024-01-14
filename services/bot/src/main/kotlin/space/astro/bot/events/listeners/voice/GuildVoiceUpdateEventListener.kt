package space.astro.bot.events.listeners.voice

import mu.KotlinLogging
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.exceptions.HierarchyException
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.core.extentions.toConfigurationErrorDto
import space.astro.bot.events.listeners.voice.handlers.VCEventHandler
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.models.discord.SimpleMemberRolesManager
import space.astro.bot.models.discord.vc.event.VCEventData
import space.astro.shared.core.daos.GuildDao
import space.astro.shared.core.daos.TemporaryVCDao

private val log = KotlinLogging.logger {  }

@Component
class GuildVoiceUpdateEventListener(
    private val guildDao: GuildDao,
    private val temporaryVCDao: TemporaryVCDao,
    private val vcEventDetector: VCEventDetector,
    private val vcEventHandler: VCEventHandler,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher
) {

    @EventListener
    fun receiveGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        // Ignore bots and when a user doesn't switch voice channel
        if (event.member.user.isBot || event.channelJoined?.id == event.channelLeft?.id) {
            return
        }

        val guildDto = guildDao.get(event.guild.id)
            ?: return
        val temporaryVcs = temporaryVCDao.getAll(event.guild.id)

        val vcEventData = VCEventData(
            event = event,
            guildData = guildDto,
            temporaryVCs = temporaryVcs,
        )

        val memberRolesManager = SimpleMemberRolesManager(event.guild, event.member)

        val events = try {
            vcEventDetector.detectAstroVCEvents(vcEventData)
        } catch (e: IllegalStateException) {
            log.error { e.message }
            return
        }

        vcEventHandler.handleEvents(events, memberRolesManager)

        try {
            memberRolesManager.queue()
        } catch (e: HierarchyException) {
            configurationErrorEventPublisher.publishConfigurationErrorEvent(
                guildId = event.guild.id,
                configurationErrorData = e.toConfigurationErrorDto()
            )
        }
    }
}