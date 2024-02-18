package space.astro.support.bot.components.discord

import dev.minn.jda.ktx.jdabuilder.injectKTX
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.SessionController
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import space.astro.shared.core.services.redis.RedisClientService
import space.astro.support.bot.components.jda.JdaToSpringEventBridge
import space.astro.support.bot.config.DiscordApplicationConfig
import space.astro.support.bot.config.PodConfig
import space.astro.support.bot.models.discord.RedisSessionController

private val log = KotlinLogging.logger { }

@Component
class ShardManagerFactory(
    private val shardManagerConfig: ShardManagerConfig,
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val jdaToSpringEventBridge: JdaToSpringEventBridge,
    private val redisClientService: RedisClientService,
) {

    private val intents = listOf(
        GatewayIntent.GUILD_MEMBERS
    )

    @Bean
    fun getDefaultShardManager(
        podConfig: PodConfig,
        sessionController: SessionController
    ): ShardManager {
        // NOTE: this is a formula for distributing shards for easier rollouts (not needed if we increase by double)

//        val shardList = IntRange(0, (shardManagerConfig.totalShards / shardManagerConfig.totalPods - 1))
//            .toList()
//            .map { it * shardManagerConfig.totalPods + podConfig.getParsedOrdinal() }

        val shardsPerPod = shardManagerConfig.totalShards / shardManagerConfig.totalPods
        val shardList = IntRange(
            podConfig.getParsedOrdinal() * shardsPerPod,
            ((podConfig.getParsedOrdinal() + 1) * shardsPerPod) - 1
        ).toList()

        log.info {
            "Starting pod ${podConfig.getParsedOrdinal()} with shards ${
                shardList.joinToString(
                    ", "
                )
            } (total: ${shardList.size}/${shardManagerConfig.totalShards})"
        }

        val activity = when (discordApplicationConfig.activityType) {
            "WATCHING" -> Activity.watching(discordApplicationConfig.activityText)
            "LISTENING" -> Activity.listening(discordApplicationConfig.activityText)
            "PLAYING" -> Activity.playing(discordApplicationConfig.activityText)
            else -> Activity.watching(discordApplicationConfig.activityText)
        }
        return DefaultShardManagerBuilder
            .createDefault(
                discordApplicationConfig.token,
                intents
            )
            .setSessionController(sessionController)
            .setShardsTotal(shardManagerConfig.totalShards)
            .setShards(shardList)
            .setActivity(activity)
            .addEventListeners(jdaToSpringEventBridge)
            .injectKTX()
            .build(false)
    }

    @Bean
    fun getRedisSessionController(): SessionController {
        return RedisSessionController(
            discordApplicationConfig,
            shardManagerConfig,
            redisClientService
        )
    }

}
