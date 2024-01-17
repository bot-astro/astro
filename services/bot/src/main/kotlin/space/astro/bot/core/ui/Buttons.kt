package space.astro.bot.core.ui

import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import space.astro.bot.interactions.InteractionIds
import space.astro.shared.core.util.ui.Links

object Buttons {
//    val website = Button.link(Links.WEBSITE, "Website")
//    val dashboard = Button.link(Links.DASHBOARD, "Dashboard")
//    val docs = Button.link(Links.Documentation.DOCUMENTATION, "Docs")
    val invite = Button.link(Links.INVITE, "Invite")
    val support = Button.link(Links.SUPPORT_SERVER, "Support")
    val premium = Button.link(Links.APP_DIRECTORY_PREMIUM, "Premium")

//    object Docs {
//        val premium = Button.link(Links.Documentation.PREMIUM, "Premium docs")
//        val variables = Button.link(Links.Documentation.VARIABLES, "Variable docs")
//        val connection = Button.link(Links.Documentation.CONNECTION, "Connection docs")
//        val generator = Button.link(Links.Documentation.GENERATOR, "Generator docs")
//        val interfaces = Button.link(Links.Documentation.INTERFACE, "Interface docs")
//        val template = Button.link(Links.Documentation.TEMPLATE, "Template docs")
//    }

    object Help {
        val general = Button.of(ButtonStyle.PRIMARY, InteractionIds.Button.HELP_GENERAL, Emojis.help)
        val premium = Button.of(ButtonStyle.PRIMARY, InteractionIds.Button.HELP_PREMIUM, Emojis.premium)
        val variables = Button.of(ButtonStyle.PRIMARY, InteractionIds.Button.HELP_VARIABLES, Emojis.variables)
        val generators = Button.of(ButtonStyle.PRIMARY, InteractionIds.Button.HELP_GENERATORS, Emojis.generator)
        val interfaces = Button.of(ButtonStyle.PRIMARY, InteractionIds.Button.HELP_INTERFACES, Emojis.vcInterface)
        val templates = Button.of(ButtonStyle.PRIMARY, InteractionIds.Button.HELP_TEMPLATES, Emojis.template)
        val connections = Button.of(ButtonStyle.PRIMARY, InteractionIds.Button.HELP_CONNECTIONS, Emojis.connection)
    }

    object Bundles {
        val help = listOf(Help.general, invite, support, premium)

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