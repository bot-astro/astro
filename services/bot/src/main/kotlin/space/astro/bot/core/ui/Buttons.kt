package space.astro.bot.core.ui

import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import space.astro.bot.interactions.InteractionIds
import space.astro.shared.core.util.ui.Links

object Buttons {
    val invite = Button.link(Links.INVITE, "Invite")
    val support = Button.link(Links.SUPPORT_SERVER, "Support")
    val ultimate = Button.link(Links.ULTIMATE, "Ultimate")
    val appDirectoryUltimate = Button.link(Links.APP_DIRECTORY_ULTIMATE, "Ultimate")
    val dashboard = Button.link(Links.DASHBOARD, "Dashboard")
    fun guildDashboard(guildId: String) = Button.link(Links.GUILD_DASHBOARD(guildId), "Dashboard")

    object Guides {
        val all = Button.link(Links.GUIDES, "Guides")
        val interfaces = Button.link(Links.GUIDES + "/interface", "Interface guide")
    }

    object Bundles {
        ////////////////////
        /// PREDASHBOARD ///
        ////////////////////
        fun confirmation(dangerous: Boolean) = listOf(cancel(), confirm(dangerous))
    }

    private fun confirm(dangerous: Boolean) = Button.of(
        if (dangerous) ButtonStyle.DANGER else ButtonStyle.SUCCESS,
        InteractionIds.getRandom(),
        "Confirm"
    )
    fun cancel() = Button.secondary(InteractionIds.getRandom(), "Cancel")
    private fun accept() = Button.success(InteractionIds.getRandom(), "Accept")
    private fun deny() = Button.danger(InteractionIds.getRandom(), "Deny")
}