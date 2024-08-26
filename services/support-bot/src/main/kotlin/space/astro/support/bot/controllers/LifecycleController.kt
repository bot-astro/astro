package space.astro.support.bot.controllers

import kotlinx.coroutines.delay
import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {  }

@RestController
class LifecycleController(
    private val shardManager: ShardManager
) {

    @GetMapping("/ready")
    suspend fun ready(): ResponseEntity<*> {
        // only send 204 if all shards are ready on this pod
        // otherwise: ResponseEntity.badRequest().build<Any>()
        val shardsReady = shardManager.shards
            .map { it.status }
            .count { it == JDA.Status.LOADING_SUBSYSTEMS || it == JDA.Status.CONNECTED }


        return if (shardsReady >= 1) {
            ResponseEntity.noContent().build<Any>()
        } else {
            log.info("Getting probed /ready: ${shardManager.shards.count { it.status == JDA.Status.CONNECTED }} / ${shardManager.shards.size}")
            log.info("Not Ready --> Returning 500")
            ResponseEntity.status(500).build<Any>()
        }
    }

    @GetMapping("/shutdown")
    suspend fun shutdown(): ResponseEntity<*> {
        log.info("Got shutdown request - persisting players...")
        delay(3000)
        return ResponseEntity.noContent().build<Any>()
    }
}