package space.astro.shared.core.components.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import mu.KotlinLogging
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger { }

/**
 * Service for communication with Redis
 */
@Component
class RedisFactory(redisConfig: RedisConfig) {

    private var client: RedisClient? = null
    private var clusterClient: RedisClusterClient? = null
    private var isCluster = false
    private var statefulRedisClusterConnection: StatefulRedisClusterConnection<String, String>? =
        null

    private var statefulRedisConnection: StatefulRedisConnection<String, String>? = null

    init {
        logger.info { "Initializing Redis connection" }
        logger.info { "Redis cluster: ${redisConfig.cluster}" }
        logger.info { "Redis Host: ${redisConfig.host}" }
        logger.info { "Redis Password: ${redisConfig.password}" }

        val uriBuilder = RedisURI.builder()
            .withHost(redisConfig.host)
            .withPort(redisConfig.port)
            .withDatabase(redisConfig.database)

        if (redisConfig.password.isNotEmpty()) {
            uriBuilder.withPassword(redisConfig.password.toCharArray())
        }

        val uri = uriBuilder.build()

        isCluster = redisConfig.cluster
        if (isCluster) {
            clusterClient = RedisClusterClient.create(uri)
            statefulRedisClusterConnection = clusterClient?.connect()
        } else {
            client = RedisClient.create(uri)
            statefulRedisConnection = client?.connect()
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
