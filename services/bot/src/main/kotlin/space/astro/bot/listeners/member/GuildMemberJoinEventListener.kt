package space.astro.bot.listeners.member

import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class GuildMemberJoinEventListener {

    @EventListener
    fun receiveGuildMemberJoinEvent(event: GuildMemberJoinEvent) {

    }
}