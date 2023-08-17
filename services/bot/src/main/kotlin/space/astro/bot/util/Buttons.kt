package space.astro.bot.util

import net.dv8tion.jda.api.interactions.components.buttons.Button
import space.astro.shared.core.util.Links

object Buttons {
    val website = Button.link(Links.base, "Website")
    val dashboard = Button.link(Links.dashboard, "Dashboard")
    val invite = Button.link(Links.invite, "Invite")
    val docs = Button.link(Links.docs, "Docs")
    val support = Button.link(Links.support, "Support")
    val premium = Button.link(Links.appDirectoryPremium, "Premium")

    object Bundles {
        val help = listOf(invite, website, docs, support, premium)
    }
}