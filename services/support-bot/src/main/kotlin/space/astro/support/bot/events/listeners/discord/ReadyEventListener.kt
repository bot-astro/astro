package space.astro.support.bot.events.listeners.discord

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class ReadyEventListener {
    @EventListener
    fun onReady(event: ReadyEvent) {
        log.info { "Logged in shard ${event.jda.shardInfo.shardId} as ${event.jda.selfUser.name}" }
    }
}
