package space.astro.bot.ui

import net.dv8tion.jda.api.components.buttons.Button
import space.astro.bot.commands.setup.SetupButton
import space.astro.shared.core.lang.L
import java.util.Locale

object Buttons {
    fun setup(locale: Locale) = Button.success(SetupButton.ID, L.t(locale, "button.setup")).withEmoji(Emojis.setup)

    fun dashboard(locale: Locale) = Button.link(Links.DASHBOARD, L.t(locale, "button.dashboard")).withEmoji(Emojis.dashboard)

    fun guides(locale: Locale) = Button.link(Links.GUIDES, L.t(locale, "button.guides")).withEmoji(Emojis.help)

    fun support(locale: Locale) = Button.link(Links.SUPPORT_SERVER, L.t(locale, "button.support")).withEmoji(Emojis.helper)

    fun ultimate(locale: Locale) = Button.link(Links.APP_DIRECTORY_ULTIMATE, L.t(locale, "button.ultimate")).withEmoji(Emojis.premium)

    fun github(locale: Locale) = Button.link(Links.GITHUB, L.t(locale, "button.github")).withEmoji(Emojis.developer)

    fun guildDashboard(
        locale: Locale,
        guildId: String,
    ) = Button.link(Links.guildDashboard(guildId), L.t(locale, "button.dashboard")).withEmoji(Emojis.dashboard)
}
