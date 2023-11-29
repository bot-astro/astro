package space.astro.bot.command.impl.info

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.command.AbstractCommand
import space.astro.bot.command.BaseCommand
import space.astro.bot.command.Command
import space.astro.bot.command.CommandContext
import space.astro.bot.core.ui.Messages

@Command(
    name = "dashboard",
    description = "Shows some instructions about the bot dashboard"
)
class DashboardCommand : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        ctx: CommandContext
    ) {
        event.reply(Messages.dashboardSettings()).queue()
    }
}