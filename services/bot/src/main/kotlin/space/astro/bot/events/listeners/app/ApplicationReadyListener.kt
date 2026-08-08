package space.astro.bot.events.listeners

import io.github.oshai.kotlinlogging.KotlinLogging
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import javax.security.auth.login.LoginException

private val log = KotlinLogging.logger {  }

@Component
class ApplicationReadyListener(
    private val shardManager: ShardManager
) {
    @EventListener(ApplicationReadyEvent::class)
    fun ready() {
        shardManager.login()
    }
}