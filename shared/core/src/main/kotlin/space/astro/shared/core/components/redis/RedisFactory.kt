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
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.time.Duration

private val logger = KotlinLogging.logger { }

/**
 * Service for communication with Redis
 */
@Component
class RedisFactory(redisConfig: RedisConfig) {

    private var statefulRedisClusterConnection: StatefulRedisClusterConnection<String, String>? =
        null

    private var statefulRedisConnection: StatefulRedisConnection<String, String>? = null

    init {
        logger.info { "Initializing Redis connection" }
        logger.info { "Redis cluster: ${redisConfig.cluster}" }
        logger.info { "Redis URIs: ${redisConfig.uris}" }

        val redisUris = redisConfig.uris.split(",").stream()
            .map(RedisURI::create)
            .toList()

        logger.info { "Redis URIs: $redisUris" }

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
            logger.info { "Creating Redis client" }
            logger.info { "Redis URI: ${redisUris[0]}" }
            logger.info { "Redis Port: ${redisUris[0].port}" }
            logger.info { "Redis Host: ${redisUris[0].host}" }
            logger.info { "Redis Password: ${redisUris[0].password.joinToString()}" }
            logger.info { "Redis Database: ${redisUris[0].database}" }
            logger.info { "Redis Username: ${redisUris[0].username}" }
            val client = RedisClient.create(redisUris[0])
            statefulRedisConnection = client.connect()
        }

    }

    @Bean
    fun reactiveCommands(
        redisConfig: RedisConfig
    ): RedisClusterReactiveCommands<String, String> {
        return if (redisConfig.cluster) {
            statefulRedisClusterConnection!!.reactive()
        } else {
            statefulRedisConnection!!.reactive()
        }
    }

    @Bean
    fun asyncCommands(
        redisConfig: RedisConfig
    ): RedisClusterAsyncCommands<String, String> {
        return if (redisConfig.cluster) {
            statefulRedisClusterConnection!!.async()
        } else {
            statefulRedisConnection!!.async()
        }
    }

    @Bean
    fun syncCommands(
        redisConfig: RedisConfig
    ): RedisClusterCommands<String, String> {
        return if (redisConfig.cluster) {
            statefulRedisClusterConnection!!.sync()
        } else {
            statefulRedisConnection!!.sync()
        }
    }
}
