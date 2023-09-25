package space.astro.bot.listeners.voice

import mu.KotlinLogging
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.exceptions.HierarchyException
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.listeners.voice.handlers.VCEventHandler
import space.astro.bot.managers.roles.SimpleMemberRolesManager
import space.astro.bot.managers.util.GuildErrorNotifier
import space.astro.bot.managers.vc.VCEventData
import space.astro.bot.managers.vc.VCEventDetector
import space.astro.shared.core.services.dao.GuildDao
import space.astro.shared.core.services.dao.TemporaryVCDao

private val log = KotlinLogging.logger {  }

@Component
class GuildVoiceUpdateEventListener(
    val guildDao: GuildDao,
    val temporaryVCDao: TemporaryVCDao,
    val vcEventDetector: VCEventDetector,
    val vcEventHandler: VCEventHandler
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

        val guildErrorNotifier = GuildErrorNotifier()
        val memberRolesManager = SimpleMemberRolesManager(event.guild, event.member)

        val events = try {
            vcEventDetector.detectAstroVCEvents(vcEventData)
        } catch (e: IllegalStateException) {
            log.error { e.message }
            return
        }

        vcEventHandler.handleEvents(events, guildErrorNotifier, memberRolesManager)

        memberRolesManager.queue {
            when (it) {
                is InsufficientPermissionException -> {}
                is HierarchyException -> {}
                is IllegalArgumentException -> {}
            }
        }
    }
}