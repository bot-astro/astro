package space.astro.shared.core.components.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.TimeoutOptions
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.ClusterClientOptions
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import java.time.Duration

@Service
class RedisFactory(redisConfig: RedisConfig) {

    private var statefulRedisClusterConnection: StatefulRedisClusterConnection<String, String>? =
        null

    private var statefulRedisConnection: StatefulRedisConnection<String, String>? = null

    init {
        val redisUris = redisConfig.uris.split(",").stream()
            .map(RedisURI::create)
            .toList()

        if (redisConfig.cluster) {
            val clusterClient = RedisClusterClient.create(redisUris)
            clusterClient.setOptions(
                ClusterClientOptions.builder()
                    .timeoutOptions(
                        TimeoutOptions.builder()
                            .fixedTimeout(Duration.ofSeconds(5))
                            .build()
                    )
                    .topologyRefreshOptions(
                        ClusterTopologyRefreshOptions.builder()
                            .enableAllAdaptiveRefreshTriggers()
                            .enablePeriodicRefresh()
                            .refreshTriggersReconnectAttempts(3)
                            .build()
                    ).build()
            )
            statefulRedisClusterConnection = clusterClient.connect()
        } else {
            val client = RedisClient.create(redisUris[0])
            statefulRedisConnection = client.connect()
        }

    }

    @Bean
    fun reactiveCommands(
        redisConfig: RedisConfig
    ): RedisClusterReactiveCommands<String, String>? {
        return if (redisConfig.cluster) {
            statefulRedisClusterConnection!!.reactive()
        } else {
            statefulRedisConnection!!.reactive()
        }
    }

    @Bean
    fun asyncCommands(
        redisConfig: RedisConfig
    ): RedisClusterAsyncCommands<String, String>? {
        return if (redisConfig.cluster) {
            statefulRedisClusterConnection!!.async()
        } else {
            statefulRedisConnection!!.async()
        }
    }
}
