package space.astro.bot.components.health

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

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
        return shardManager.statuses.values.all { HEALTHY_STATUSES.contains(it) }
    }

    fun anyStuck(threshold: Duration): Boolean {
        return timestamps.any { Clock.System.now() - it.value > threshold }
    }

}
