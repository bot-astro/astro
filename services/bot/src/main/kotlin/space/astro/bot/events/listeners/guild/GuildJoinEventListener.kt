package space.astro.bot.events.listeners.guild

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.guild.GuildJoinEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import space.astro.bot.models.analytics.GuildEventData
import space.astro.bot.models.discord.PermissionSets
import space.astro.bot.ui.Embeds
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

        // TODO: components v2, replace the embed (last step of the migration)
        // TODO: premium, add back the "Ultimate" and "Vote" buttons when premium is implemented
        // TODO: add the "Setup" button depending on what is decided for the setup commands
        channel
            ?.sendMessageEmbeds(createWelcomeEmbed(guild))
            ?.setComponents(
                ActionRow.of(
                    Button.link(Links.guildDashboard(guild.id), "Dashboard").withEmoji(Emojis.dashboard),
                    Button.link(Links.GUIDES, "Guides").withEmoji(Emojis.help),
                    Button.link(Links.SUPPORT_SERVER, "Support").withEmoji(Emojis.helper),
                ),
            )?.queue()
    }

    private fun createWelcomeEmbed(guild: Guild): MessageEmbed {
        val selfAvatar = guild.selfMember.user.effectiveAvatarUrl

        return EmbedBuilder()
            .setColor(Embeds.purple)
            .setAuthor("Astro just landed in ${guild.name}!", Links.guildDashboard(guild.id), selfAvatar)
            .setDescription("Thanks for inviting Astro in this amazing server, let's get it to the next level now!")
            .setThumbnail(guild.iconUrl ?: selfAvatar)
            .addField(
                "Features",
                "Astro provides the following features:" +
                    "\n${Emojis.generator.formatted} *Temporary voice channels*" +
                    "\n${Emojis.voiceRole.formatted} *Voice roles*" +
                    "\n${Emojis.vcInterface.formatted} *Interfaces*" +
                    "\n${Emojis.template.formatted} *Templates*" +
                    "\nTo find out all the cool things you can do with Astro, take a look at the [`Guides`](${Links.GUIDES})!",
                false,
            ).addField(
                "Setup",
                "Use the ${Emojis.dashboard.formatted} [`Dashboard`](${Links.guildDashboard(guild.id)}) to setup Astro and configure its settings!",
                false,
            ).addField(
                "Support",
                "You can join the ${Emojis.helper.formatted} [`Support server`](${Links.SUPPORT_SERVER}) to ask questions if something is not quite clear to you!",
                false,
            ).setFooter("Thank you for using Astro!")
            .build()
    }
}
