package space.astro.bot.core.ui

import net.dv8tion.jda.api.entities.SkuSnowflake
import net.dv8tion.jda.api.interactions.components.buttons.Button
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle
import space.astro.bot.interactions.InteractionIds
import space.astro.shared.core.util.ui.Links

object Buttons {
    val invite = Button.link(Links.INVITE, "Invite").withEmoji(Emojis.invite)
    val support = Button.link(Links.SUPPORT_SERVER, "Support").withEmoji(Emojis.helper)
    val ultimate = Button.link(Links.ULTIMATE, "Ultimate").withEmoji(Emojis.premium)
    val appDirectoryUltimate = Button.link(Links.APP_DIRECTORY_ULTIMATE, "Ultimate").withEmoji(Emojis.premium)
    val dashboard = Button.link(Links.DASHBOARD, "Dashboard").withEmoji(Emojis.dashboard)
    fun guildDashboard(guildId: String) = Button.link(Links.GUILD_DASHBOARD(guildId), "Dashboard").withEmoji(Emojis.dashboard)
    val vote = Button.link(Links.VOTE, "Vote").withEmoji(Emojis.vote)
    val github = Button.link(Links.GITHUB, "GitHub").withEmoji(Emojis.developer)

    object Guides {
        val all = Button.link(Links.GUIDES, "Guides").withEmoji(Emojis.help)
        val interfaces = Button.link(Links.GUIDES + "/interface", "Interface guide").withEmoji(Emojis.vcInterface)
    }

    val premiumRequired = Button.premium(SkuSnowflake.fromId("1096107722115661934")) // yeah... this should be injected from env variables

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