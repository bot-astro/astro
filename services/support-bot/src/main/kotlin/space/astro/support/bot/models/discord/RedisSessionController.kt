package space.astro.support.bot.models.discord

import com.google.common.util.concurrent.ThreadFactoryBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.utils.SessionController
import net.dv8tion.jda.api.utils.SessionController.ShardedGateway
import net.dv8tion.jda.api.utils.SessionControllerAdapter
import space.astro.shared.core.models.redis.RedisKey
import space.astro.shared.core.models.redis.RedisRateLimiter
import space.astro.shared.core.services.redis.RedisClientService
import space.astro.support.bot.components.discord.ShardManagerConfig
import space.astro.support.bot.config.DiscordApplicationConfig
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.Future

class RedisSessionController(
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val shardManagerConfig: ShardManagerConfig,
    private val redisClientService: RedisClientService
) : SessionControllerAdapter() {

    companion object {
        private val CONNECT_INTERVAL = Duration.ofSeconds(7)
    }

    private val connectionNodes = mutableMapOf<SessionController.SessionConnectNode, Future<*>>()

    private val rateLimiter = RedisRateLimiter(redisClientService.reactiveCommands(), "IDENTIFY", 1, CONNECT_INTERVAL)

    private val executor = Executors.newCachedThreadPool(
        ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("session-controller-%d")
            .build()
    )

    override fun appendSession(node: SessionController.SessionConnectNode) {
        val shardId = node.shardInfo.shardId
        val ratelimitShard: Int = shardId % shardManagerConfig.loginFactor
        val key = getKey(discordApplicationConfig.botId, ratelimitShard)

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

    override fun getGlobalRatelimit(): Long {
        return try {
            redisClientService.asyncCommands().get(getRedisGlobalRatelimitKey(discordApplicationConfig.botId)).get()
                .toLong()
        } catch (e: Exception) {
            Long.MIN_VALUE
        }
    }

    override fun setGlobalRatelimit(ratelimit: Long) {
        redisClientService.asyncCommands()
            .set(getRedisGlobalRatelimitKey(discordApplicationConfig.botId), ratelimit.toString()).get()
    }

    override fun getShardedGateway(api: JDA): ShardedGateway {
        return ShardedGateway(gateway, -1)
    }

    private fun getKey(botId: Long, ratelimitShard: Int): String {
        return listOf(botId.toString(), ratelimitShard.toString()).joinToString(":")
    }

    private fun getRedisGlobalRatelimitKey(botId: Long): String {
        return RedisKey.GLOBAL_RATELIMIT.key.format(botId)
    }
}
