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
import space.astro.bot.utils.extensions.supportedLocale
import space.astro.bot.utils.extensions.toPermissionList
import space.astro.shared.core.lang.L
import space.astro.shared.core.repositories.mongo.GuildSettingsRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale

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

        channel?.sendMessage(createWelcomeMessage(guild, locale))?.queue()
    }

    private fun createWelcomeMessage(
        guild: Guild,
        locale: Locale,
    ): MessageCreateData {
        val selfAvatar = guild.selfMember.user.effectiveAvatarUrl
        val dashboard = Links.guildDashboard(guild.id)

        return Containers.message(
            Containers.container(
                Containers.purple,
                Containers.section(
                    Thumbnail.fromUrl(guild.iconUrl ?: selfAvatar),
                    L.t(locale, "welcome.title", "guild" to guild.name, "link" to dashboard),
                    L.t(locale, "welcome.description"),
                ),
                Containers.field(
                    L.t(locale, "help.features.title"),
                    L.t(
                        locale,
                        "help.features.content",
                        "generatorEmoji" to Emojis.generator.formatted,
                        "voiceRoleEmoji" to Emojis.voiceRole.formatted,
                        "interfaceEmoji" to Emojis.vcInterface.formatted,
                        "templateEmoji" to Emojis.template.formatted,
                        "guidesLink" to Links.GUIDES,
                    ),
                ),
                Containers.field(
                    L.t(locale, "welcome.setup.title"),
                    L.t(locale, "help.setup.content", "emoji" to Emojis.dashboard.formatted, "link" to dashboard),
                ),
                Containers.field(
                    L.t(locale, "help.support.title"),
                    L.t(locale, "help.support.content", "emoji" to Emojis.helper.formatted, "link" to Links.SUPPORT_SERVER),
                ),
            ),
            ActionRow.of(
                Buttons.guildDashboard(locale, guild.id),
                Buttons.guides(locale),
                Buttons.support(locale),
            ),
        )
    }
}
