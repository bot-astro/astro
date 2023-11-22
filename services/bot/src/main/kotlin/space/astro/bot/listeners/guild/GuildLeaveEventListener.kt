package space.astro.bot.listeners.guild

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class GuildLeaveEventListener {

    @EventListener
    fun receiveGuildLeaveEvent(event: GuildLeaveEvent) {
        TODO("BIGQUERY")
    }
}