package space.astro.shared.core.models.redis

import io.lettuce.core.RedisNoScriptException
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands
import space.astro.shared.core.models.ratelimit.RateLimiter
import space.astro.shared.core.models.ratelimit.RateLimiterAcquireResult
import java.time.Duration

class RedisRateLimiter(
    private val commands: RedisClusterReactiveCommands<String, String>,
    private val namespace: String,
    private val defaultMaxPerInterval: Int,
    private val intervalDuration: Duration
) : RateLimiter {

    private var scriptHash: String

    companion object {
        private const val SCRIPT = "" +
                "local count = redis.call(\"incr\",KEYS[1])\n" +
                "local ttl = redis.call(\"pttl\",KEYS[1])\n" +
                "if ttl == -1 then\n" +
                "    redis.call(\"pexpire\",KEYS[1],ARGV[1])\n" +
                "    return { count, tonumber(ARGV[1]) }\n" +
                "elseif ttl == -2 then\n" +
                "    return { count, 0 }\n" +
                "else" +
                "    return { count, ttl }\n" +
                "end\n" +
                ";"
    }

    init {
        scriptHash = commands.scriptLoad(SCRIPT).block()!!
    }

    override fun tryAcquire(id: String): RateLimiterAcquireResult {
        return tryAcquire(id, defaultMaxPerInterval)
    }

    override fun tryAcquire(id: String, maxPerInterval: Int): RateLimiterAcquireResult {
        val key = getKey(id)

        val keys = arrayOf(key)
        val args = arrayOf(intervalDuration.toMillis().toString())

        return try {
            val result = commands.evalsha<ArrayList<Long>>(scriptHash, ScriptOutputType.MULTI, keys, *args)
                .collectList()
                .block()!!

            val data = result[0]
            val incr = data[0]

            RateLimiterAcquireResult(
                acquired = incr <= maxPerInterval,
                count = incr,
                ttl = data[1]
            )
        } catch (e: RedisNoScriptException) {
            e.printStackTrace()
            // NOTE: recreate script if redis fails to find old one
            scriptHash = commands.scriptLoad(SCRIPT).block()!!

            // NOTE: try to prevent outage by releasing rate limit temporarily
            RateLimiterAcquireResult(
                acquired = true
            )
        } catch (e: Exception) {
            e.printStackTrace()

            // NOTE: try to prevent outage by releasing rate limit temporarily
            RateLimiterAcquireResult(
                acquired = true
            )
        }
    }

    private fun getKey(key: String): String {
        return listOf("RATELIMIT", namespace, key).joinToString(":")
    }

}
