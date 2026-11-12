package space.astro.support.bot.events.listeners.app

import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class ApplicationReadyListener(
    private val shardManager: ShardManager,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun ready() {
        shardManager.login()
    }
}
