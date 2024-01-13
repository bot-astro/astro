package space.astro.bot.interactions.handlers.command.impl.predashboard

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.GeneratorSettingsInteractionContext
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.Command
import space.astro.bot.interactions.handlers.command.CommandCategory
import space.astro.bot.interactions.handlers.command.SubCommand

@Command(
    name = "generator-waiting",
    description = "Manage the generator waiting room settings",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.SETTINGS,
    action = InteractionAction.SETTINGS
)
class GeneratorWaitingCommand : AbstractCommand() {
    @SubCommand(
        name = "bitrate",
        description = "Set the default bitrate for waiting rooms"
    )
    fun bitrate(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "category",
        description = "Set the category where waiting vcs get generated"
    )
    fun category(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "creation",
        description = "Choose whether Astro should create waiting vcs automatically"
    )
    fun creation(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "limit",
        description = "Set the default user limit for waiting vcs"
    )
    fun limit(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "name",
        description = "The name for waiting vcs (see docs for variables)"
    )
    fun name(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "permissions",
        description = "Set from which channel waiting vcs should inherit the permissions"
    )
    fun permissions(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "position",
        description = "Set the position where waiting vcs will be created relative to their temporary vc"
    )
    fun position(
        event: SlashCommandInteractionEvent,
        ctx: GeneratorSettingsInteractionContext
    ) {

    }
}