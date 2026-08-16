package space.astro.bot.sharding

import io.lettuce.core.api.sync.RedisCommands
import net.dv8tion.jda.api.utils.SessionController
import net.dv8tion.jda.api.utils.SessionControllerAdapter
import org.springframework.stereotype.Component
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.bot.config.ShardManagerConfig
import space.astro.shared.core.utils.ratelimit.RedisRateLimiter
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * This class is used to ensure shard gateway identification is handled safely respecting rate limits.
 *
 * For details [Discord API Sharding docs](https://docs.discord.com/developers/events/gateway#sharding)
 */
@Component
class RedisShardSessionController(
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val shardManagerConfig: ShardManagerConfig,
    redisCommands: RedisCommands<String, String>,
) : SessionControllerAdapter() {

    companion object {
        // slightly higher than Discord 5s limit
        private val CONNECT_INTERVAL = Duration.ofSeconds(7)
    }

    private val connectionNodes = mutableMapOf<SessionController.SessionConnectNode, Future<*>>()

    private val rateLimiter = RedisRateLimiter(redisCommands, "IDENTIFY", 1, CONNECT_INTERVAL)

    // multiple shards will append their connection
    // since appending a connection is a blocking operation, because we wait for the rate limiting on the bucket
    // we need to use threads to parallelize this
    // additionally, daemon threads won't prevent JVM shutdown
    private val threadFactory = Thread.ofPlatform()
        .daemon(true)
        .name("session-controller-", 0)
        .factory()
    // we use a cache pool because we don't want shards to wait for other shards that belong to a different bucket
    // if we had a fixed number of threads, a shard might have to wait for a shard of a different bucket to acquire a
    // rate limit
    // this way instead each shard gets its thread and doesn't need to wait
    private val executor = Executors.newCachedThreadPool(threadFactory)

    override fun appendSession(node: SessionController.SessionConnectNode) {
        val shardId = node.shardInfo.shardId
        val concurrencyBucket: Int = shardId % shardManagerConfig.maxIdentifyConcurrency
        val key = getBucketIdentifier(discordApplicationConfig.id, concurrencyBucket)

        val future: Future<*> = executor.submit {
            try {
                rateLimiter.acquire(key)
                node.run(false)
            } catch (e: Exception) {
                log.error("Error while running session connect node", e)
                appendSession(node)
            }
        }

        connectionNodes[node] = future
    }

    override fun removeSession(node: SessionController.SessionConnectNode) {
        connectionNodes.remove(node)?.cancel(true)
    }

    private fun getBucketIdentifier(applicationId: Long, concurrencyBucket: Int): String {
        return listOf(applicationId.toString(), concurrencyBucket.toString()).joinToString(":")
    }

}
