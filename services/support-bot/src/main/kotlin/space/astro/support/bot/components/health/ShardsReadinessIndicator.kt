package space.astro.support.bot.components.health

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("shardsReadiness")
class ShardsReadinessIndicator(
    private val shardManager: ShardManager,
) : HealthIndicator {
    companion object {
        private val HEALTHY_STATUSES =
            setOf(
                JDA.Status.CONNECTED,
                JDA.Status.LOADING_SUBSYSTEMS,
            )
    }

    override fun health(): Health =
        try {
            // there are no shards until the login
            val shards = shardManager.shards
            if (shards.isNotEmpty() && shards.all { it.status in HEALTHY_STATUSES }) Health.up().build() else Health.down().build()
        } catch (_: Exception) {
            Health.down().build()
        }
}
