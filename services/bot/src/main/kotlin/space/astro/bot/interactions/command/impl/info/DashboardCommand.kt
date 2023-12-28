package space.astro.bot.interactions.command.impl.info

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.command.AbstractCommand
import space.astro.bot.interactions.command.BaseCommand
import space.astro.bot.interactions.command.Command
import space.astro.bot.interactions.InteractionContext
import space.astro.bot.core.ui.Messages

@Command(
    name = "dashboard",
    description = "Shows some instructions about the bot dashboard"
)
class DashboardCommand : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext
    ) {
        event.reply(Messages.dashboardSettings()).queue()
    }
}