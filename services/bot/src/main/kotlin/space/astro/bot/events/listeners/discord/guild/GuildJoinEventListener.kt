package space.astro.bot.events.listeners.discord.guild

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.thumbnail.Thumbnail
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.models.analytics.GuildEventData
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.ui.Buttons
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis
import space.astro.bot.ui.Links
import space.astro.bot.utils.extensions.toPermissionList
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Component
class GuildJoinEventListener(
    private val applicationEventPublisher: ApplicationEventPublisher,
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

        // TODO: premium, add back the "Ultimate" and "Vote" buttons when premium is implemented
        // TODO: add the "Setup" button depending on what is decided for the setup commands
        channel?.sendMessage(createWelcomeMessage(guild))?.queue()
    }

    private fun createWelcomeMessage(guild: Guild): MessageCreateData {
        val selfAvatar = guild.selfMember.user.effectiveAvatarUrl
        val dashboard = Links.guildDashboard(guild.id)

        return Containers.message(
            Containers.container(
                Containers.purple,
                Containers.section(
                    Thumbnail.fromUrl(guild.iconUrl ?: selfAvatar),
                    "## Astro just landed in [${guild.name}]($dashboard)!",
                    "Thanks for inviting Astro in this amazing server, let's get it to the next level now!",
                ),
                Containers.divider(),
                Containers.field(
                    "Features",
                    "Astro provides the following features:" +
                        "\n${Emojis.generator.formatted} *Temporary voice channels*" +
                        "\n${Emojis.voiceRole.formatted} *Voice roles*" +
                        "\n${Emojis.vcInterface.formatted} *Interfaces*" +
                        "\n${Emojis.template.formatted} *Templates*" +
                        "\nTo find out all the cool things you can do with Astro, take a look at the [`Guides`](${Links.GUIDES})!",
                ),
                Containers.field(
                    "Setup",
                    "Use the ${Emojis.dashboard.formatted} [`Dashboard`]($dashboard) to setup Astro and configure its settings!",
                ),
                Containers.field(
                    "Support",
                    "You can join the ${Emojis.helper.formatted} [`Support server`](${Links.SUPPORT_SERVER}) to ask questions if something is not quite clear to you!",
                ),
                Containers.divider(),
                ActionRow.of(
                    Buttons.guildDashboard(guild.id),
                    Buttons.guides,
                    Buttons.support,
                ),
                Containers.footer("Thank you for using Astro!"),
            ),
        )
    }
}
