package space.astro.shared.core.services.redis

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
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Service
import space.astro.shared.core.components.redis.RedisConfig
import java.time.Duration

@Service
class RedisClientService(
    redisConfig: RedisConfig
) {

    private var isCluster = false
    private lateinit var client: RedisClient
    private lateinit var clusterClient: RedisClusterClient
    private var connection: StatefulRedisConnection<String, String>? = null
    private var clusterConnection: StatefulRedisClusterConnection<String, String>? = null

    init {
        val redisUris = redisConfig.uris.split(",").stream()
            .map(RedisURI::create)
            .toList()

        if (redisConfig.cluster) {
            clusterClient = RedisClusterClient.create(redisUris)
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
            clusterConnection = clusterClient.connect()
        } else {
            client = RedisClient.create(redisUris[0])
            connection = client.connect()
        }
    }

    fun asyncCommands(): RedisClusterAsyncCommands<String, String> {
        return if (isCluster) clusterConnection!!.async() else connection!!.async()
    }

    fun syncCommands(): RedisClusterCommands<String, String> {
        return if (isCluster) clusterConnection!!.sync() else connection!!.sync()
    }

    fun reactiveCommands(): RedisClusterReactiveCommands<String, String> {
        return if (isCluster) clusterConnection!!.reactive() else connection!!.reactive()
    }

}
