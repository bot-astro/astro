package space.astro.bot.interactions.handlers.command.impl.predashboard

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.Command
import space.astro.bot.interactions.handlers.command.CommandCategory
import space.astro.bot.interactions.handlers.command.SubCommand

@Command(
    name = "settings",
    description = "Edit Astro's settings in your server",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.SETTINGS
)
class SettingsCommand : AbstractCommand() {
    @SubCommand(
        name = "errors",
        description = "Shows configuration errors detected by Astro"
    )
    fun errors(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "clean-temporary-voice-channels",
        description = "Removes any left over temporary voice channel"
    )
    fun cleanVcs(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {

    }
}