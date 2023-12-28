package space.astro.bot.events.listeners.guild

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.shared.core.models.analytics.AnalyticsEvent
import space.astro.shared.core.models.analytics.AnalyticsEventReceiver
import space.astro.shared.core.models.analytics.AnalyticsEventType
import space.astro.shared.core.models.analytics.GuildEventData
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class GuildLeaveEventListener(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    @EventListener
    fun receiveGuildLeaveEvent(event: GuildLeaveEvent) {
        val guild = event.guild

        val analyticsEvent = AnalyticsEvent(
            receivers = listOf(AnalyticsEventReceiver.BIGQUERY),
            type = AnalyticsEventType.GUILD_EVENT,
            data = GuildEventData(
                guildId = guild.idLong,
                usersCount = guild.memberCount,
                action = GuildEventData.GuildEventAction.KICKED,
                timestamp = LocalDateTime.now(ZoneOffset.UTC).atOffset(ZoneOffset.UTC).toString()
            )
        )

        applicationEventPublisher.publishEvent(analyticsEvent)
    }
}