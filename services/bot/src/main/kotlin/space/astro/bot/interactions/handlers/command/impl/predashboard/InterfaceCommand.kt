package space.astro.bot.interactions.handlers.command.impl.predashboard

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.InterfaceSettingsInteractionContext
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.Command
import space.astro.bot.interactions.handlers.command.CommandCategory
import space.astro.bot.interactions.handlers.command.SubCommand

@Command(
    name = "interface",
    description = "Create, edit and delete interfaces",
    requiredPermissions = [Permission.MANAGE_CHANNEL],
    category = CommandCategory.SETTINGS,
    action = InteractionAction.SETTINGS
)
class InterfaceCommand : AbstractCommand() {

    /////////////
    /// BASIC ///
    /////////////
    @SubCommand(
        name = "create",
        description = "Creates an interface that can be used to manage temporary voice channels"
    )
    fun create(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "delete",
        description = "Deletes an interface"
    )
    fun delete(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext
    ) {

    }

    ////////////
    /// EDIT ///
    ////////////
    @SubCommand(
        name = "button",
        description = "Adds a button to an interface",
        group = "add",
        groupDescription = "0"
    )
    fun addButton(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "button",
        description = "Deletes a button from an interface",
        group = "delete",
        groupDescription = "0"
    )
    fun deleteButton(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "button-order",
        description = "Changes the order of the buttons in an interface",
        group = "edit",
        groupDescription = "0"
    )
    fun editButtonOrder(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "button",
        description = "Edit a button of an interface",
        group = "edit",
        groupDescription = "0"
    )
    fun editButton(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "channel",
        description = "Moves an interface to another channel",
        group = "edit",
        groupDescription = "0"
    )
    fun editChannel(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "message",
        description = "Modify the message content and style of an interface",
        group = "edit",
        groupDescription = "0"
    )
    fun editMessage(
        event: SlashCommandInteractionEvent,
        ctx: InterfaceSettingsInteractionContext
    ) {

    }


}