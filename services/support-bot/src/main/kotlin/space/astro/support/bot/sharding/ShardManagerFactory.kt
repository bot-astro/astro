package space.astro.support.bot.sharding

import dev.minn.jda.ktx.jdabuilder.injectKTX
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder
import net.dv8tion.jda.api.sharding.ShardManager
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import space.astro.support.bot.events.JdaToSpringEventBridge
import space.astro.support.bot.properties.DiscordApplicationProperties

@Component
class ShardManagerFactory(
    private val discordApplicationProperties: DiscordApplicationProperties,
    private val jdaToSpringEventBridge: JdaToSpringEventBridge,
) {
    private val intents =
        listOf(
            GatewayIntent.GUILD_MEMBERS,
        )

    @Bean
    fun getDefaultShardManager(): ShardManager {
        val activity =
            Activity.of(discordApplicationProperties.getActivityType(), discordApplicationProperties.activityText)

        return DefaultShardManagerBuilder
            .createLight(
                discordApplicationProperties.token,
                intents,
            ).setShardsTotal(1)
            .setActivity(activity)
            .addEventListeners(jdaToSpringEventBridge)
            .injectKTX()
            .build(false)
    }
}
