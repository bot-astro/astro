package space.astro.shared.core.services.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger { }

/**
 * Service for communication with Redis
 */
@Service
class RedisClientService(redisConfig: RedisConfig) {

    private var isCluster = false
    private var client: RedisClient? = null
    private var clusterClient: RedisClusterClient? = null
    private var connection: StatefulRedisConnection<String, String>? = null
    private var clusterConnection: StatefulRedisClusterConnection<String, String>? = null

    // TODO: add topology refresh configuration
    init {
        logger.info { "Initializing Redis client" }
        logger.info { "Using Redis host '${redisConfig.host}' and port '${redisConfig.port}'" }
        val uriBuilder = RedisURI.builder()
            .withHost(redisConfig.host)
            .withPort(redisConfig.port)
            .withDatabase(redisConfig.database)

        if (redisConfig.password != null) {
            logger.info { "Using password '${redisConfig.password}' for Redis connection" }
            uriBuilder.withPassword(redisConfig.password)
        }

        val uri = uriBuilder.build()

        isCluster = redisConfig.cluster
        if (isCluster) {
            logger.info { "Using Redis cluster" }
            clusterClient = RedisClusterClient.create(uri)
            clusterConnection = clusterClient?.connect()
        } else {
            logger.info { "Using single Redis instance" }
            logger.info { "Uri: $uri" }
            client = RedisClient.create(uri)
            connection = client?.connect()
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
