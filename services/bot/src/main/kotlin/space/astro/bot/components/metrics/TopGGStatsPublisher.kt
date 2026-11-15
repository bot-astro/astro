package space.astro.bot.components.metrics

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import space.astro.bot.properties.DiscordApplicationProperties
import space.astro.bot.properties.PodProperties
import space.astro.shared.core.clients.DiscordApiClient
import space.astro.shared.core.clients.TopGGClient
import space.astro.shared.core.exceptions.AException
import space.astro.shared.core.properties.bot.BotShardProperties
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }

/**
 * Pushes the guild and shard count of the whole bot to top.gg.
 *
 * The guild count comes from Discord's application endpoint, which already covers every shard, so only the pod
 * running shard 0 (ordinal 0) needs to push.
 */
@Component
@ConditionalOnProperty(prefix = "topgg", name = ["enabled"], havingValue = "true")
class TopGGStatsPublisher(
    private val discordApiClient: DiscordApiClient,
    private val topGGClient: TopGGClient,
    private val discordApplicationProperties: DiscordApplicationProperties,
    private val podProperties: PodProperties,
    private val botShardProperties: BotShardProperties,
) {
    @Scheduled(
        initialDelayString = "\${topgg.interval-hours:12}",
        fixedDelayString = "\${topgg.interval-hours:12}",
        timeUnit = TimeUnit.HOURS,
    )
    fun publish() {
        if (podProperties.getParsedOrdinal() != 0) return

        try {
            val guildCount = discordApiClient.getApplication(discordApplicationProperties.token).approximateGuildCount

            if (guildCount == null) {
                log.warn { "Discord did not return the approximate guild count, skipping the top.gg push" }
                return
            }

            topGGClient.pushMetrics(guildCount, botShardProperties.totalShards)
        } catch (e: AException) {
            log.error(e) { "Failed pushing the bot statistics to top.gg" }
        }
    }
}
