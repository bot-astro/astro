package space.astro.bot.components.metrics

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.properties.PodProperties

@Component
class ShardGauges(
    private val registry: MeterRegistry,
    private val podProperties: PodProperties,
) {
    @EventListener
    fun onReady(event: ReadyEvent) {
        val shard = event.jda

        register("discord.shards", shard) { it.status.ordinal.toDouble() }
        register("discord.guilds", shard) { it.guilds.size.toDouble() }
        register("discord.unavailable_guilds", shard) { it.unavailableGuilds.size.toDouble() }
        register("discord.gateway.ping", shard) { it.gatewayPing.toDouble() }
    }

    private fun register(
        name: String,
        shard: JDA,
        value: (JDA) -> Double,
    ) {
        Gauge
            .builder(name, shard) { value(it) }
            .tag("pod_id", podProperties.getParsedOrdinal().toString())
            .tag("shard_id", shard.shardInfo.shardId.toString())
            .register(registry)
    }
}
