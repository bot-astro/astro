package space.astro.bot.interactions.handlers.command.impl.predashboard

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.context.TemplateSettingsInteractionContext
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.Command
import space.astro.bot.interactions.handlers.command.CommandCategory
import space.astro.bot.interactions.handlers.command.SubCommand

@Command(
    name = "template",
    description = "Create predefined channel templates and use them anywhere",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.SETTINGS,
    action = InteractionAction.SETTINGS
)
class TemplateCommand : AbstractCommand() {

    //////////////
    /// BASICS ///
    //////////////
    @SubCommand(
        name = "create",
        description = "Create a template that can be easely assigned to voice channels"
    )
    fun create(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "delete",
        description = "Deletes a template"
    )
    fun delete(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "enabled-generators",
        description = "Set which generators can use a certain template"
    )
    fun enabledGenerators(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext
    ) {

    }


    ////////////
    /// EDIT ///
    ////////////
    @SubCommand(
        name = "bitrate",
        description = "Set the bitrate for a vc template",
        group = "edit",
        groupDescription = "0"
    )
    fun editBitrate(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "limit",
        description = "Set the user limit for a vc template",
        group = "edit",
        groupDescription = "0"
    )
    fun editLimit(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "name",
        description = "Set the name for a vc template",
        group = "edit",
        groupDescription = "0"
    )
    fun editName(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "region",
        description = "Set the region for a vc template",
        group = "edit",
        groupDescription = "0"
    )
    fun editRegion(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "template-name",
        description = "Modify the name of the actual template",
        group = "edit",
        groupDescription = "0"
    )
    fun editTemplateName(
        event: SlashCommandInteractionEvent,
        ctx: TemplateSettingsInteractionContext
    ) {

    }
}