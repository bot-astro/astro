package space.astro.bot.sharding

import io.lettuce.core.api.sync.RedisCommands
import net.dv8tion.jda.api.requests.RestRateLimiter
import net.dv8tion.jda.api.utils.SessionController
import net.dv8tion.jda.api.utils.SessionControllerAdapter
import org.springframework.stereotype.Component
import space.astro.bot.properties.DiscordApplicationProperties
import space.astro.shared.core.utils.ratelimit.RedisRateLimiter
import space.astro.shared.core.utils.redis.RedisKey
import java.time.Duration
import java.util.concurrent.CancellationException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Handles shards identification respecting rate limits.
 * Needed when deploying on multiple pods in kubernetes.
 *
 * Discord allows 1 identify request each 5s per concurrency bucket, a shard belongs to the bucket
 * `shard_id % max_concurrency`.
 * Example with `max_concurrency = 16` and 160 shards: shards 0, 16, 32, ... 144 share bucket 0 and identify one at a
 * time, while shards 0, 1, 2, ... 15 (one per bucket) identify in parallel.
 *
 * `max_concurrency` is not configured manually, Discord provides it in `GET /gateway/bot`
 * (`session_start_limit.max_concurrency`) and JDA passes it to [setConcurrency] before the first shard connects.
 *
 * The state of each bucket lives in Redis, so shards of different pods that belong to the same bucket wait for each
 * other.
 *
 * This class also shares the REST global rate limit state between pods, see [RedisGlobalRateLimit].
 *
 * For details [Discord API Sharding docs](https://docs.discord.com/developers/events/gateway#sharding)
 */
@Component
class RedisShardSessionController(
    private val discordApplicationProperties: DiscordApplicationProperties,
    redisCommands: RedisCommands<String, String>,
) : SessionControllerAdapter() {
    companion object {
        // slightly higher than Discord 5s limit
        private val CONNECT_INTERVAL = Duration.ofSeconds(7)
    }

    /**
     * Number of concurrency buckets, set by JDA through [setConcurrency] with the value provided by Discord.
     *
     * The thread starting the first shard will set this value. Because threads might keep their own cache
     * of variables, other threads might keep use their cached `1` value instead of reading the updated value
     * wrote by the first thread.
     * With `@Volatile` we explicitly tell the JVM to make those write available to other threads.
     */
    @Volatile
    private var maxConcurrency = 1

    private val connectionNodes = ConcurrentHashMap<SessionController.SessionConnectNode, Future<*>>()

    private val rateLimiter = RedisRateLimiter(redisCommands, "IDENTIFY", 1, CONNECT_INTERVAL)

    private val globalRateLimit = RedisGlobalRateLimit(redisCommands, discordApplicationProperties.id)

    // multiple shards will append their connection
    // since appending a connection is a blocking operation, because we wait for the rate limiting on the bucket
    // we need to use threads to parallelize this
    // additionally, daemon threads won't prevent JVM shutdown
    private val threadFactory =
        Thread
            .ofPlatform()
            .daemon(true)
            .name("session-controller-", 0)
            .factory()

    // we use a cache pool because we don't want shards to wait for other shards that belong to a different bucket
    // if we had a fixed number of threads, a shard might have to wait for a shard of a different bucket to acquire a
    // rate limit
    // this way instead each shard gets its thread and doesn't need to wait
    private val executor = Executors.newCachedThreadPool(threadFactory)

    /**
     * Called by JDA with the `max_concurrency` value returned by Discord in `GET /gateway/bot`,
     * before any shard is connected.
     */
    override fun setConcurrency(level: Int) {
        log.info("Discord max identify concurrency: {}", level)
        maxConcurrency = level
    }

    override fun getRateLimitHandle(): RestRateLimiter.GlobalRateLimit = globalRateLimit

    override fun appendSession(node: SessionController.SessionConnectNode) {
        val shardId = node.shardInfo.shardId
        val concurrencyBucket: Int = shardId % maxConcurrency
        val key =
            RedisKey.SHARD_BUCKET_ID(
                applicationId = discordApplicationProperties.id.toString(),
                bucketId = concurrencyBucket.toString(),
            )

        val future: Future<*> =
            executor.submit {
                try {
                    rateLimiter.acquire(key)
                    node.run(false)
                } catch (_: InterruptedException) {
                    // the session was removed (see removeSession), it must not be queued again
                } catch (_: CancellationException) {
                    // same as above
                } catch (e: Exception) {
                    log.error("Error while running session connect node", e)
                    appendSession(node)
                }
            }

        connectionNodes.put(node, future)?.cancel(true)
    }

    override fun removeSession(node: SessionController.SessionConnectNode) {
        connectionNodes.remove(node)?.cancel(true)
    }
}
