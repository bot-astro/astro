package space.astro.bot.ui

import net.dv8tion.jda.api.components.buttons.Button
import space.astro.bot.commands.ai.AiApproveButton
import space.astro.bot.commands.ai.AiCancelButton
import space.astro.bot.commands.ai.AiMessageButton
import space.astro.bot.commands.setup.CreateGeneratorButton
import space.astro.bot.commands.setup.CreateInterfaceButton
import space.astro.bot.commands.setup.SetupButton
import space.astro.shared.core.lang.L
import java.util.Locale

object Buttons {
    fun setup(locale: Locale) = Button.success(SetupButton.ID, L.t(locale, "button.setup")).withEmoji(Emojis.setup)

    fun createGenerator(locale: Locale) =
        Button.primary(CreateGeneratorButton.ID, L.t(locale, "button.create_generator")).withEmoji(Emojis.generator)

    fun createInterface(locale: Locale) =
        Button.primary(CreateInterfaceButton.ID, L.t(locale, "button.create_interface")).withEmoji(Emojis.vcInterface)

    fun aiMessage(locale: Locale) = Button.primary(AiMessageButton.ID, L.t(locale, "button.ai_message")).withEmoji(Emojis.chat)

    fun aiApprove(
        locale: Locale,
        proposalId: String,
    ) = Button.success("${AiApproveButton.ID}?$proposalId", L.t(locale, "button.ai_approve"))

    fun aiCancel(
        locale: Locale,
        proposalId: String,
    ) = Button.secondary("${AiCancelButton.ID}?$proposalId", L.t(locale, "button.ai_cancel"))

    fun dashboard(locale: Locale) = Button.link(Links.DASHBOARD, L.t(locale, "button.dashboard")).withEmoji(Emojis.dashboard)

    fun guides(locale: Locale) = Button.link(Links.GUIDES, L.t(locale, "button.guides")).withEmoji(Emojis.help)

    fun support(locale: Locale) = Button.link(Links.SUPPORT_SERVER, L.t(locale, "button.support")).withEmoji(Emojis.helper)

    fun ultimate(locale: Locale) = Button.link(Links.APP_DIRECTORY_ULTIMATE, L.t(locale, "button.ultimate")).withEmoji(Emojis.premium)

    fun github(locale: Locale) = Button.link(Links.GITHUB, L.t(locale, "button.github")).withEmoji(Emojis.developer)

    fun guildErrors(
        locale: Locale,
        guildId: String,
    ) = Button.link(Links.guildErrors(guildId), L.t(locale, "button.errors")).withEmoji(Emojis.logs)

    fun guildDashboard(
        locale: Locale,
        guildId: String,
    ) = Button.link(Links.guildDashboard(guildId), L.t(locale, "button.dashboard")).withEmoji(Emojis.dashboard)
}
