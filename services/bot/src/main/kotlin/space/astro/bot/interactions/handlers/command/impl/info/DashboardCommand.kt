package space.astro.bot.interactions.handlers.command.impl.info

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.core.ui.Messages
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.BaseCommand
import space.astro.bot.interactions.handlers.command.Command

@Command(
    name = "dashboard",
    description = "points you to the dashboard where you can configure the bot settings for your server"
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