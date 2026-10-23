package space.astro.bot.ui

import net.dv8tion.jda.api.components.buttons.Button

object Buttons {
    val dashboard = Button.link(Links.DASHBOARD, "Dashboard").withEmoji(Emojis.dashboard)
    val guides = Button.link(Links.GUIDES, "Guides").withEmoji(Emojis.help)
    val support = Button.link(Links.SUPPORT_SERVER, "Support").withEmoji(Emojis.helper)
    val github = Button.link(Links.GITHUB, "GitHub").withEmoji(Emojis.developer)

    fun guildDashboard(guildId: String) = Button.link(Links.guildDashboard(guildId), "Dashboard").withEmoji(Emojis.dashboard)
}
