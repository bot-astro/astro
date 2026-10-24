package space.astro.bot.components.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.components.health.ShardsHealthTracker
import space.astro.bot.properties.PodProperties

enum class PodStatus {
    OK,
    UNHEALTHY,
}

@Component
class PodGauge(
    private val registry: MeterRegistry,
    private val shardManager: ShardManager,
    private val podProperties: PodProperties,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        Gauge
            .builder("discord.pods", shardManager) { manager ->
                val healthy = manager.shards.all { it.status in ShardsHealthTracker.HEALTHY_STATUSES }
                (if (healthy) PodStatus.OK else PodStatus.UNHEALTHY).ordinal.toDouble()
            }.tag("pod_id", podProperties.getParsedOrdinal().toString())
            .register(registry)
    }
}
