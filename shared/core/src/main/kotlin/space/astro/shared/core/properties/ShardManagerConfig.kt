package space.astro.shared.core.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties("shard.manager")
class ShardManagerConfig {

    /** Total number of shards across all pods */
    val totalShards: Int = 1

    /** Total number of pods */
    val totalPods: Int = 1

    /** Number of IDENTIFY requests allowed per 5 seconds
     *
     * See [Discord API docs](https://docs.discord.com/developers/events/gateway#sharding)
     */
    val maxIdentifyConcurrency: Int = 1
}
