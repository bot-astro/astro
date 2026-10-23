package space.astro.bot.events.listeners.guild

import net.dv8tion.jda.api.events.guild.GuildLeaveEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.models.analytics.GuildEventData
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class GuildLeaveEventListener(
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    @EventListener
    fun guildLeaveReceived(event: GuildLeaveEvent) {
        val guild = event.guild

        applicationEventPublisher.publishEvent(
            GuildEventData(
                guildId = guild.idLong,
                usersCount = guild.memberCount,
                action = GuildEventData.GuildEventAction.KICKED,
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            ),
        )
    }
}
