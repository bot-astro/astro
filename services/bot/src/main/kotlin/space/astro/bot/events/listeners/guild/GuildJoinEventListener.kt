package space.astro.bot.events.listeners.guild

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.core.extentions.toPermissionList
import space.astro.bot.models.discord.PermissionSets
import space.astro.shared.core.models.analytics.AnalyticsEvent
import space.astro.shared.core.models.analytics.AnalyticsEventReceiver
import space.astro.shared.core.models.analytics.AnalyticsEventType
import space.astro.shared.core.models.analytics.GuildEventData
import space.astro.shared.core.util.extention.linkFromLink
import space.astro.shared.core.util.ui.Colors
import space.astro.shared.core.util.ui.Links
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class GuildJoinEventListener(
    private val applicationEventPublisher: ApplicationEventPublisher
) {

    @EventListener
    fun receiveGuildJoinEvent(event: GuildJoinEvent) {
        val guild = event.guild

        trackGuildJoinAnalyticEvent(guild)

        val channel = guild.systemChannel
            ?.takeIf {
                guild.selfMember.hasPermission(it, PermissionSets.astroSendMessagePermissions.toPermissionList())
            }
            ?: guild.textChannels.firstOrNull {
                guild.selfMember.hasPermission(it, PermissionSets.astroSendMessagePermissions.toPermissionList())
            }

        channel
            ?.sendMessageEmbeds(createGuildJoinMessage(guild))
            /* TODO: Buttons
            ?.setComponents(
                ActionRow.of(
                    Buttons.buttonOfAction(SetupA()),
                    Buttons.buttonOfAction(GeneratorCreateA(), ButtonStyle.PRIMARY, "Create Generator", null),
                    Buttons.buttonOfAction(CommandsA(), ButtonStyle.PRIMARY),
                    Buttons.buttonOfAction(HelpA(), ButtonStyle.PRIMARY)
                ), Buttons.Rows.links
            )
             */
            ?.queue()
    }

    private fun trackGuildJoinAnalyticEvent(guild: Guild) {
        val analyticsEvent = AnalyticsEvent(
            receivers = listOf(AnalyticsEventReceiver.BIGQUERY),
            type = AnalyticsEventType.GUILD_EVENT,
            data = GuildEventData(
                guildId = guild.idLong,
                usersCount = guild.memberCount,
                action = GuildEventData.GuildEventAction.JOINED,
                timestamp = LocalDateTime.now(ZoneOffset.UTC).atOffset(ZoneOffset.UTC).toString()
            )
        )

        applicationEventPublisher.publishEvent(analyticsEvent)
    }

    private fun createGuildJoinMessage(guild: Guild): MessageEmbed {
        return Embed(
            color = Colors.purple.rgb,
            authorName = "Astro just landed in ${guild.name}!",
            authorUrl = Links.WEBSITE,
            authorIcon = guild.selfMember.user.effectiveAvatarUrl,
            description = "Thanks for inviting Astro in this amazing server, let's get started making it better now!",
            thumbnail = guild.iconUrl ?: guild.selfMember.user.effectiveAvatarUrl,
            fields = listOf(
                MessageEmbed.Field(
                    "Setup",
                    "Open the ${Links.DASHBOARD.linkFromLink("dashboard")} and configure Astro!",
                    false
                ),
                // TODO: Command mention
                MessageEmbed.Field(
                    "Help & other resources",
                    "You can find some general information about Astro in the", // ${HelpSC().asMention()} command.",
                    false
                )
            ),
            footerText = "Developed by the ${Links.DEVELOPERS.linkFromLink("Astro team")}",
        )
    }
}