package space.astro.bot.events.listeners.guild

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.core.extentions.toPermissionList
import space.astro.bot.core.ui.Buttons
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionComponentBuilder
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.models.discord.PermissionSets
import space.astro.shared.core.models.analytics.AnalyticsEvent
import space.astro.shared.core.models.analytics.AnalyticsEventReceiver
import space.astro.shared.core.models.analytics.AnalyticsEventType
import space.astro.shared.core.models.analytics.GuildEventData
import space.astro.shared.core.util.ui.Colors
import space.astro.shared.core.util.ui.Links
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class GuildJoinEventListener(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val interactionComponentBuilder: InteractionComponentBuilder
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
            ?.setComponents(
                ActionRow.of(
                    interactionComponentBuilder.buttonWithLabelAndEmoji(
                        id = InteractionIds.Button.SETUP,
                        buttonStyle = ButtonStyle.SUCCESS,
                        label = "Setup",
                        emoji = Emojis.setup
                    ),
                    interactionComponentBuilder.buttonWithLabelAndEmoji(
                        id = InteractionIds.Button.HELP_GENERAL,
                        buttonStyle = ButtonStyle.PRIMARY,
                        label = "Help",
                        emoji = Emojis.help
                    ),
                    Buttons.support,
                    Buttons.premium,
                )
            )
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
                    "Use the ${Emojis.setup.formatted} Setup button below to setup Astro for your server!",
                    false
                ),
                MessageEmbed.Field(
                    "Help & other resources",
                    "You can find some general information about Astro with `/help`.",
                    false
                )
            ),
            footerText = "Thank you for using Astro!",
        )
    }

    /*
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
                MessageEmbed.Field(
                    "Help & other resources",
                    "You can find some general information about Astro with `/help`.",
                    false
                )
            ),
            footerText = "Developed by the ${Links.DEVELOPERS.linkFromLink("Astro team")}",
        )
    }
     */
}