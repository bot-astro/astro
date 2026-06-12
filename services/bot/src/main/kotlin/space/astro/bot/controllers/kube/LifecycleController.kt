package space.astro.bot.controllers.kube

import kotlinx.coroutines.delay
import mu.KotlinLogging
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import space.astro.shared.core.components.web.BotApiRoutes
import kotlin.time.Duration.Companion.milliseconds

private val log = KotlinLogging.logger { }

@RestController
class LifecycleController(
    val shardManager: ShardManager
) {
    @GetMapping("/dummy")
    suspend fun dummy(@RequestHeader("Authorization") auth: String): ResponseEntity<*> {
        log.info("Got dummy request")
        return ResponseEntity.noContent().build<Any>()
    }

    @GetMapping(BotApiRoutes.Kube.SHUTDOWN)
    suspend fun shutdown(@RequestHeader("Authorization") auth: String): ResponseEntity<*> {
        log.info("Got shutdown request - persisting players...")
        delay(3000.milliseconds)
        return ResponseEntity.noContent().build<Any>()
    }
}
