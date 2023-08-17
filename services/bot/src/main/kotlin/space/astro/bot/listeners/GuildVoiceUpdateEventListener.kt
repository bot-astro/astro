package space.astro.bot.listeners

import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.VoiceDispatchInterceptor.VoiceStateUpdate
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class GuildVoiceUpdateEventListener() {

    @EventListener
    fun receiveGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {

    }
}