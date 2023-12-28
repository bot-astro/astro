package space.astro.bot.core.ui

import net.dv8tion.jda.api.interactions.components.buttons.Button
import space.astro.shared.core.util.ui.Links

object Buttons {
    val website = Button.link(Links.WEBSITE, "Website")
    val dashboard = Button.link(Links.DASHBOARD, "Dashboard")
    val invite = Button.link(Links.INVITE, "Invite")
    val docs = Button.link(Links.Documentation.DOCUMENTATION, "Docs")
    val support = Button.link(Links.SUPPORT_SERVER, "Support")
    val premium = Button.link(Links.APP_DIRECTORY_PREMIUM, "Premium")

    object Bundles {
        val help = listOf(invite, website, docs, support, premium)
    }
}