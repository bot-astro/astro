package space.astro.bot.events.listeners

import mu.KotlinLogging
import net.dv8tion.jda.api.events.session.ReadyEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.interactions.handlers.command.CommandHandler
import space.astro.shared.core.models.analytics.AnalyticsEvent
import space.astro.shared.core.models.analytics.AnalyticsEventReceiver
import space.astro.shared.core.models.analytics.AnalyticsEventType
import space.astro.shared.core.models.analytics.GuildSnapshotData
import java.time.LocalDateTime
import java.time.ZoneOffset

private val log = KotlinLogging.logger { }

@Component
class ReadyEventListener(
    private val commandHandler: CommandHandler,
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    @EventListener
    fun receiveReadyEvent(event: ReadyEvent) {
        val shardId = event.jda.shardInfo.shardId

        log.info("Logged in shard $shardId as ${event.jda.selfUser.name}")

        event.jda.guilds.forEach { guild ->
            applicationEventPublisher.publishEvent(
                AnalyticsEvent(
                    receivers = listOf(AnalyticsEventReceiver.BIGQUERY),
                    type = AnalyticsEventType.GUILD_SNAPSHOT,
                    data = GuildSnapshotData(
                        guildId = guild.idLong,
                        guildName = guild.name,
                        memberCount = guild.memberCount,
                        shardId = shardId,
                        timestamp = LocalDateTime.now(ZoneOffset.UTC)
                            .atOffset(ZoneOffset.UTC)
                            .toString()
                    )
                )
            )
        }

        if (shardId == 0) {
            log.info("Upserting commands because we're on shard 0")
            commandHandler.registerCommands()
        }
    }
}
