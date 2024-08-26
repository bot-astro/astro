package space.astro.bot.controllers.kube

import kotlinx.coroutines.delay
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import space.astro.shared.core.components.web.BotApiRoutes
import space.astro.shared.core.configs.KubeConfig

private val log = KotlinLogging.logger { }

@RestController
class LifecycleController(
    private val kubeConfig: KubeConfig,
    val shardManager: ShardManager
) {

    @GetMapping(BotApiRoutes.Kube.READY)
    suspend fun ready(@RequestHeader("Authorization") auth: String): ResponseEntity<*> {
        // only send 204 if all shards are ready on this pod
        // otherwise: ResponseEntity.badRequest().build<Any>()
        val allShardsReady: Boolean = shardManager.shards
            .map { it.status }
            .all { it == JDA.Status.LOADING_SUBSYSTEMS || it == JDA.Status.CONNECTED }

        return if (allShardsReady) {
            ResponseEntity.noContent().build<Any>()
        } else {
            log.info("Getting probed /ready: ${shardManager.shards.count { it.status == JDA.Status.CONNECTED }} / ${shardManager.shards.size}")
            log.info("Not Ready --> Returning 500")
            ResponseEntity.status(500).build<Any>()
        }
    }

    @GetMapping(BotApiRoutes.Kube.SHUTDOWN)
    suspend fun shutdown(@RequestHeader("Authorization") auth: String): ResponseEntity<*> {
        log.info("Got shutdown request - persisting players...")
        delay(3000)
        return ResponseEntity.noContent().build<Any>()
    }
}
