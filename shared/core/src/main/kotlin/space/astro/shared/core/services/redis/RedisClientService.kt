package space.astro.shared.core.services.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.cluster.RedisClusterClient
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands
import io.lettuce.core.cluster.api.sync.RedisClusterCommands
import org.springframework.stereotype.Service

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
        val uriBuilder = RedisURI.builder()
            .withHost(redisConfig.host)
            .withPort(redisConfig.port)
            .withDatabase(redisConfig.database)

        if (redisConfig.password != null) {
            uriBuilder.withPassword(redisConfig.password)
        }

        val uri = uriBuilder.build()

        isCluster = redisConfig.cluster
        if (isCluster) {
            clusterClient = RedisClusterClient.create(uri)
            clusterConnection = clusterClient?.connect()
        } else {
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
