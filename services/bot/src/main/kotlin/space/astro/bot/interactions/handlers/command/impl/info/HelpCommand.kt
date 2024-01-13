package space.astro.bot.interactions.handlers.command.impl.info

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.components.ActionRow
import space.astro.bot.core.ui.Buttons
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.BaseCommand
import space.astro.bot.interactions.handlers.command.Command


@Command(
    name = "help",
    description = "get started using Astro"
)
class HelpCommand : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext
    ) {
        ctx.replyHandler.reply(
            embed = Embeds.help,
            components = listOf(ActionRow.of(Buttons.Bundles.help))
        )
    }
}