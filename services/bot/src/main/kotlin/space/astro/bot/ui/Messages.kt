package space.astro.bot.ui

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.thumbnail.Thumbnail
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.utils.messages.MessageCreateData
import space.astro.shared.core.lang.L
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
                Buttons.guildDashboard(locale, guild.id),
                Buttons.guides(locale),
                Buttons.support(locale),
            ),
        )
    }

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
