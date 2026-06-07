package space.astro.bot.components.health

import mu.KotlinLogging
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private val log = KotlinLogging.logger { }

@Component
class ShardsHealthTracker(
    val shardManager: ShardManager
) {

    companion object {
        val HEALTHY_STATUSES = setOf(
            JDA.Status.CONNECTED,
            JDA.Status.LOADING_SUBSYSTEMS
        )
    }

    val timestamps = ConcurrentHashMap<Int, Instant>()

    @Scheduled(fixedRate = 30, timeUnit = TimeUnit.SECONDS)
    fun check() {
        shardManager.statuses.forEach { (shard, status) ->
            val id = shard.shardInfo.shardId

            if (HEALTHY_STATUSES.contains(status)) {
                timestamps.remove(id)
            } else {
                timestamps.putIfAbsent(id, Clock.System.now())
            }
        }
    }

    fun allHealthy(): Boolean {
        val notHealthy = shardManager.shards.filter { !HEALTHY_STATUSES.contains(it.status) }
        if (notHealthy.isNotEmpty())
            log.warn("Shards not healthy: ${notHealthy.joinToString { it.shardInfo.shardId.toString() }}")
        return notHealthy.isEmpty()
    }

    fun anyStuck(threshold: Duration): Boolean {
        val stuckShards = timestamps.filter { Clock.System.now() - it.value > threshold }
        if (stuckShards.isNotEmpty())
            log.warn("Shards stuck: ${stuckShards.keys.joinToString { it.toString() }}")
        return stuckShards.isNotEmpty()
    }

}
