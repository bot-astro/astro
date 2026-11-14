package space.astro.bot.events.listeners.discord.guild

import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.models.analytics.GuildEventData
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class GuildJoinEventListener(
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    // The welcome message is disabled: spam-check bots flag it and kick Astro within seconds of joining
    @EventListener
    fun guildJoinReceived(event: GuildJoinEvent) {
        val guild = event.guild

        applicationEventPublisher.publishEvent(
            GuildEventData(
                guildId = guild.idLong,
                usersCount = guild.memberCount,
                action = GuildEventData.GuildEventAction.JOINED,
                timestamp = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            ),
        )
    }
}
