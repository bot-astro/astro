package space.astro.bot.interactions.handlers.button.impl.help

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import space.astro.bot.core.ui.Buttons
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.button.AbstractButton
import space.astro.bot.interactions.handlers.button.Button
import space.astro.bot.interactions.handlers.button.ButtonRunnable

@Button(
    id = InteractionIds.Button.HELP_PREMIUM,
)
class HelpPremiumButton : AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        ctx: InteractionContext
    ) {
        ctx.replyHandler.setEphemeral(false)
        ctx.replyHandler.replyEmbedAndComponent(
            embed = Embeds.helpPremium,
            component = Buttons.premium
        )
    }
}