package space.astro.bot.interactions.handlers.button.impl.info

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import space.astro.bot.core.ui.Buttons
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.button.Button
import space.astro.bot.interactions.handlers.button.ButtonRunnable

@Button(
    id = InteractionIds.Button.HELP,
)
class HelpButton : space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        ctx: InteractionContext
    ) {
        ctx.replyHandler.reply(
            embed = Embeds.help,
            components = listOf(ActionRow.of(Buttons.Bundles.help))
        )
    }
}