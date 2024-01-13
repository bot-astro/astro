package space.astro.bot.events.listeners

import mu.KotlinLogging
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.interactions.handlers.command.CommandHandler

private val log = KotlinLogging.logger {  }

@Component
class ReadyEventListener(
    private val commandHandler: CommandHandler
) {

    @EventListener
    fun receiveReadyEvent(event: ReadyEvent) {
        log.info("Logged in as ${event.jda.selfUser.name}")

        val shardId = event.jda.shardInfo.shardId

        if (shardId == 0) {
            log.info("Upserting commands because we're on shard 0")
            commandHandler.registerCommands()
        }
    }
}
