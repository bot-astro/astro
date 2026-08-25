package space.astro.bot.sharding

import dev.minn.jda.ktx.jdabuilder.injectKTX
import io.github.oshai.kotlinlogging.KotlinLogging
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
import space.astro.bot.properties.DiscordApplicationProperties
import space.astro.bot.properties.PodProperties
import space.astro.bot.events.publishers.ConfigurationErrorEventPublisher
import space.astro.shared.core.properties.ShardManagerConfig

private val log = KotlinLogging.logger { }

@Component
class ShardManagerFactory(
    private val shardManagerConfig: ShardManagerConfig,
    private val discordApplicationProperties: DiscordApplicationProperties,
    private val jdaToSpringEventBridge: JdaToSpringEventBridge,
    private val configurationErrorEventPublisher: ConfigurationErrorEventPublisher,
) {

    private val intents = listOf(
        GatewayIntent.GUILD_PRESENCES,
        GatewayIntent.GUILD_VOICE_STATES
    )

    @Bean
    fun getDefaultShardManager(
        podProperties: PodProperties,
        redisShardSessionController: RedisShardSessionController
    ): ShardManager {
        val shardsPerPod = shardManagerConfig.totalShards / shardManagerConfig.totalPods
        val shardList = IntRange(
            podProperties.getParsedOrdinal() * shardsPerPod,
            ((podProperties.getParsedOrdinal() + 1) * shardsPerPod) - 1
        ).toList()

        log.info {
            "Starting pod ${podProperties.getParsedOrdinal()} with shards ${
                shardList.joinToString(
                    ", "
                )
            } (total: ${shardList.size}/${shardManagerConfig.totalShards})"
        }

        val activity = Activity.of(discordApplicationProperties.getActivityType(), discordApplicationProperties.activityText)

        RestAction.setDefaultFailure(DefaultFailureConsumer(configurationErrorEventPublisher))

        return DefaultShardManagerBuilder
            .createLight(
                discordApplicationProperties.token,
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
