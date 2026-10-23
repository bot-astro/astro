package space.astro.bot.sharding

import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.sync.RedisCommands
import net.dv8tion.jda.api.requests.RestRateLimiter
import space.astro.shared.core.utils.redis.RedisKey

private val log = KotlinLogging.logger { }

/**
 * Shares JDA's REST "global" rate limit state between all the bot pods through Redis.
 *
 * JDA keeps this state in memory, so when one pod receives a global (or Cloudflare) 429 the other pods
 * keep sending requests and get more 429s, which count towards the Cloudflare invalid request limit
 * (10,000 per 10 minutes, applied per IP and pods usually share the egress IP).
 *
 * Per-route (bucket) rate limits are not shared on purpose: they are keyed by top level resource (channel, guild,
 * webhook) and a guild is always handled by a single shard, so a single pod.
 *
 * Both values are epoch milliseconds until which requests must be held back, `-1` means not limited.
 * JDA reads them every time it computes the delay of a request, so reads are served from a local copy
 * that is refreshed from Redis at most every [REFRESH_INTERVAL_MS].
 * Redis failures never block requests, the local value is used instead.
 *
 * For details [Discord API rate limits docs](https://docs.discord.com/developers/topics/rate-limits)
 */
class RedisGlobalRateLimit(
    private val commands: RedisCommands<String, String>,
    applicationId: Long,
) : RestRateLimiter.GlobalRateLimit {
    companion object {
        private const val NOT_LIMITED = -1L
        private const val REFRESH_INTERVAL_MS = 500L

        /**
         * Stores the timestamp only if it is greater than the current one, so that a shorter limit reported by a
         * pod can never replace a longer one, and makes the key expire when the limit does.
         */
        private const val SET_IF_GREATER_SCRIPT =
            "" +
                "local current = tonumber(redis.call('get', KEYS[1]))\n" +
                "if current == nil or current < tonumber(ARGV[1]) then\n" +
                "    redis.call('set', KEYS[1], ARGV[1])\n" +
                "    redis.call('pexpireat', KEYS[1], ARGV[1])\n" +
                "end\n" +
                "return 1\n"
    }

    private val classic = SharedTimestamp(RedisKey.GLOBAL_RATE_LIMIT(applicationId, "classic"))
    private val cloudflare = SharedTimestamp(RedisKey.GLOBAL_RATE_LIMIT(applicationId, "cloudflare"))

    override fun getClassic(): Long = classic.get()

    override fun setClassic(timestamp: Long) = classic.set(timestamp)

    override fun getCloudflare(): Long = cloudflare.get()

    override fun setCloudflare(timestamp: Long) = cloudflare.set(timestamp)

    private inner class SharedTimestamp(
        private val key: String,
    ) {
        /** Local copy shared across JDA threads. */
        @Volatile
        private var value = NOT_LIMITED

        /** Epoch millis of the last Redis read. */
        @Volatile
        private var lastRefresh = 0L

        fun get(): Long {
            val now = System.currentTimeMillis()

            if (now - lastRefresh >= REFRESH_INTERVAL_MS) {
                lastRefresh = now
                fetch()?.let { valueFromRedis -> value = maxOf(value, valueFromRedis) }
            }

            return value
        }

        fun set(timestamp: Long) {
            value = maxOf(value, timestamp)

            try {
                commands.eval<Long>(SET_IF_GREATER_SCRIPT, ScriptOutputType.INTEGER, arrayOf(key), timestamp.toString())
            } catch (e: Exception) {
                log.error(e) { "Failed to share global rate limit $key with the other pods" }
            }
        }

        private fun fetch(): Long? =
            try {
                commands.get(key)?.toLongOrNull()
            } catch (e: Exception) {
                log.error(e) { "Failed to read global rate limit $key" }
                null
            }
    }
}
