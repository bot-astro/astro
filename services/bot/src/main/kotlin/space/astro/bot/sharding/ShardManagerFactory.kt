package space.astro.bot.sharding

import dev.minn.jda.ktx.jdabuilder.injectKTX
import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.api.sync.RedisCommands
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.requests.RestAction
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import net.dv8tion.jda.api.utils.MemberCachePolicy
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import space.astro.bot.utils.discord.DefaultFailureConsumer
import space.astro.bot.events.JdaToSpringEventBridge
import space.astro.bot.config.DiscordApplicationConfig
import space.astro.bot.config.PodConfig
import space.astro.bot.config.ShardManagerConfig
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher

private val log = KotlinLogging.logger { }

@Component
class ShardManagerFactory(
    private val shardManagerConfig: ShardManagerConfig,
    private val discordApplicationConfig: DiscordApplicationConfig,
    private val jdaToSpringEventBridge: JdaToSpringEventBridge,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
) {

    private val intents = listOf(
        GatewayIntent.GUILD_PRESENCES,
        GatewayIntent.GUILD_VOICE_STATES
    )

    @Bean
    fun getDefaultShardManager(
        podConfig: PodConfig,
        redisShardSessionController: RedisShardSessionController
    ): ShardManager {
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

        val activity = Activity.of(discordApplicationConfig.getActivityType(), discordApplicationConfig.activityText)

        RestAction.setDefaultFailure(DefaultFailureConsumer(configurationErrorEventPublisher))

        return DefaultShardManagerBuilder
            .createLight(
                discordApplicationConfig.token,
                intents
            )
            .setMemberCachePolicy(MemberCachePolicy.VOICE)
            .enableCache(CacheFlag.VOICE_STATE, CacheFlag.MEMBER_OVERRIDES, CacheFlag.ACTIVITY)
            .setSessionController(redisShardSessionController)
            .setShardsTotal(shardManagerConfig.totalShards)
            .setShards(shardList)
            .setActivity(activity)
            .addEventListeners(jdaToSpringEventBridge)
            .injectKTX()
            .build(false)
    }
}
