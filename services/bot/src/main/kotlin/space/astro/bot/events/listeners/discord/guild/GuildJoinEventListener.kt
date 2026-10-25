package space.astro.bot.events.listeners.discord.guild

import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.models.analytics.GuildEventData
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.ui.Messages
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.bot.utils.extensions.toPermissionList
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class GuildJoinEventListener(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val guildSettingsRepository: GuildSettingsRepository,
) {
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

        val permissions = PermissionSets.astroSendMessagePermissions.toPermissionList()

        // TODO: select the channel in a better way (e.g. the one the inviter was in / most active text channel)
        val channel =
            guild.systemChannel
                ?.takeIf { guild.selfMember.hasPermission(it, permissions) }
                ?: guild.textChannels.firstOrNull { guild.selfMember.hasPermission(it, permissions) }

        val locale = guildSettingsRepository.findByGuildID(guild.id).supportedLocale

        channel?.sendMessage(Messages.welcome(guild, locale))?.queue()
    }
}
