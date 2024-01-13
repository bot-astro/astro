package space.astro.bot.interactions.handlers.command.impl.predashboard

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.BaseCommand
import space.astro.bot.interactions.handlers.command.Command
import space.astro.bot.interactions.handlers.command.CommandCategory

@Command(
    name = "setup",
    description = "Create a Generator, a connection and a error-log channel",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.SETTINGS,
    action = InteractionAction.SETTINGS
)
class SetupCommand : AbstractCommand() {
    @BaseCommand
    fun run(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {

    }
}