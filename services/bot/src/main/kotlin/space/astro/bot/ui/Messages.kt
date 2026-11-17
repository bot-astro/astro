package space.astro.bot.ui

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.thumbnail.Thumbnail
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import space.astro.shared.core.lang.L
import space.astro.shared.core.lang.localizedDescription
import space.astro.shared.core.models.database.ConfigurationErrorEntity
import java.util.Locale

object Messages {
    /**
     * Sent when Astro joins a guild
     */
    fun welcome(
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
                Buttons.setup(locale),
                Buttons.guildDashboard(locale, guild.id),
                Buttons.guides(locale),
                Buttons.support(locale),
            ),
        )
    }

    /**
     * Sent in DM to whoever invited Astro in [guild], no setup button because that one only works inside the guild
     */
    fun inviterOnboarding(
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
                    L.t(locale, "inviter_onboarding.title", "guild" to guild.name, "link" to dashboard),
                    L.t(locale, "inviter_onboarding.description"),
                ),
                Containers.field(
                    L.t(locale, "welcome.setup.title"),
                    L.t(locale, "help.setup.content", "emoji" to Emojis.dashboard.formatted, "link" to dashboard),
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

    /**
     * First message of an updates channel
     */
    fun updatesChannelReady(
        guild: Guild,
        locale: Locale,
    ): MessageCreateData =
        Containers.message(
            Containers.container(
                Containers.purple,
                Containers.title(L.t(locale, "updates_channel.ready.title")),
                Containers.text(L.t(locale, "updates_channel.ready.description")),
            ),
            ActionRow.of(Buttons.guildDashboard(locale, guild.id), Buttons.support(locale)),
        )

    /**
     * Alert about a configuration error of a guild, [occurrences] is the amount of times it happened since the last alert
     */
    fun configurationErrorAlert(
        guildId: String,
        locale: Locale,
        error: ConfigurationErrorEntity,
        occurrences: Long,
    ): MessageCreateData {
        val buttons = mutableListOf<Button>(Buttons.guildErrors(locale, guildId))
        error.code.guide?.let { buttons += Buttons.guide(locale, it) }

        val children = mutableListOf<ContainerChildComponent>()
        if (occurrences > 1) {
            children += Containers.footer(L.t(locale, "alert.occurrences", "count" to occurrences))
        }

        return Containers.message(
            Containers.container(
                Containers.red,
                Containers.title("⚠️ ${L.t(locale, "alert.configuration_error.title")}"),
                Containers.text(error.localizedDescription(locale)),
                *children.toTypedArray(),
            ),
            ActionRow.of(buttons),
        )
    }

    /**
     * Sent once when a guild reached the limit of alerts, the others are not posted until the limit resets
     */
    fun alertsHeldBack(
        guildId: String,
        locale: Locale,
    ): MessageCreateData =
        Containers.message(
            Containers.container(
                Containers.purple,
                Containers.text(L.t(locale, "alert.held_back")),
            ),
            ActionRow.of(Buttons.guildErrors(locale, guildId)),
        )

    /**
     * Log about a member in the chat of a temporary VC, [key] is the translation key of the log text
     */
    fun vcLog(
        guild: Guild,
        locale: Locale,
        key: String,
        vararg args: Pair<String, Any?>,
    ): MessageCreateData =
        Containers.message(
            Containers.container(
                guild.selfMember.colorRaw,
                Containers.title(L.t(locale, "vc.log.title", "emoji" to Emojis.logs.formatted)),
                Containers.text(L.t(locale, key, *args)),
                Containers.footer(L.t(locale, "vc.log.footer")),
            ),
        )
}
