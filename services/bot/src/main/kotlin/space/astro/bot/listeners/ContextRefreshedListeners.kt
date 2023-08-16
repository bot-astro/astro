package space.astro.bot.listeners

import mu.KotlinLogging
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import javax.security.auth.login.LoginException

private val log = KotlinLogging.logger {  }

@Component
class ContextRefreshedListeners(
    private val shardManager: ShardManager
) {
    @EventListener
    @Throws(LoginException::class)
    fun applicationStarted(event: ContextRefreshedEvent) {
        log.info { "Logging in" }
        shardManager.login()
        log.info { "Connecting to websocket" }
    }
}