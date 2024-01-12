package space.astro.support.bot.listeners

import mu.KotlinLogging
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {  }

@Component
class ReadyEventListener {

    @EventListener
    fun receiveReadyEvent(event: ReadyEvent) {
        log.info("Logged in as ${event.jda.selfUser.name}")

        /*
        val shardId = event.jda.shardInfo.shardId

        if (shardId == 0) {
            log.info("Upserting commands because we're on shard 0")
            commandHandler.registerCommands()
        }
         */
    }
}
