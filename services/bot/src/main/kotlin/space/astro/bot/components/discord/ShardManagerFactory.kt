package space.astro.bot.components.discord

import dev.minn.jda.ktx.jdabuilder.injectKTX
import io.lettuce.core.cluster.api.async.RedisClusterAsyncCommands
import io.lettuce.core.cluster.api.reactive.RedisClusterReactiveCommands
import mu.KotlinLogging
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.SessionController
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import space.astro.bot.components.jda.JdaToSpringEventBridge
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.bot.config.PodConfig
import space.astro.bot.config.ShardManagerConfig
import space.astro.bot.core.extentions.toConfigurationErrorDto
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.bot.models.discord.RedisSessionController

private val log = KotlinLogging.logger { }

@Component
class ShardManagerFactory(
    private val shardManagerConfig: ShardManagerConfig,
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val jdaToSpringEventBridge: JdaToSpringEventBridge,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
    private val asyncCommands: RedisClusterAsyncCommands<String, String>,
    private val reactiveCommands: RedisClusterReactiveCommands<String, String>
) {

    private val intents = listOf(
        GatewayIntent.GUILD_PRESENCES,
        GatewayIntent.GUILD_VOICE_STATES
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

        RestAction.setDefaultFailure {
            when (it) {
                is InsufficientPermissionException -> {
                    configurationErrorEventPublisher.publishConfigurationErrorEvent(
                        configurationErrorData = it.toConfigurationErrorDto(it.guildId.toString())
                    )
                }
            }
        }

        return DefaultShardManagerBuilder
            .createLight(
                discordApplicationConfig.token,
                intents
            )
            .setMemberCachePolicy(MemberCachePolicy.VOICE)
            .enableCache(CacheFlag.VOICE_STATE, CacheFlag.MEMBER_OVERRIDES, CacheFlag.ACTIVITY)
//            .setSessionController(sessionController)
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
            asyncCommands,
            reactiveCommands
        )
    }

}
