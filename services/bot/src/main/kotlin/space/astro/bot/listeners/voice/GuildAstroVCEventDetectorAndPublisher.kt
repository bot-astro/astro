package space.astro.bot.listeners.voice

import mu.KotlinLogging
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.managers.vc.VCEventData
import space.astro.bot.managers.vc.VCEventDetector

private val log = KotlinLogging.logger {  }

@Component
class GuildAstroVCEventDetectorAndPublisher {

    @EventListener
    fun receiveGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        // Ignore bots and when a user doesn't switch voice channel
        if (event.member.user.isBot || event.channelJoined?.id == event.channelLeft?.id) {
            return
        }

        val vcEventData = VCEventData(
            event = event,
            generators = listOf(),
            temporaryVCs = listOf(),
            connections = listOf(),
        )

        try {
            val events = VCEventDetector.detectAstroVoiceEvents(vcEventData)
            // TODO:
            // - manage events one by one
            // - create a role manager instance and pass it to each event manager
            // - queue the role manager at the end
        } catch (e: IllegalStateException) {
            log.error { e.message }
        }
    }
}