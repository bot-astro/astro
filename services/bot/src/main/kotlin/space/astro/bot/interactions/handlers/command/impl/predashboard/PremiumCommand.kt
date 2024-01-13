package space.astro.bot.interactions.handlers.command.impl.predashboard

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.context.SettingsInteractionContext
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.Command
import space.astro.bot.interactions.handlers.command.SubCommand

@Command(
    name = "premium",
    description = "Get info about premium",
)
class PremiumCommand : AbstractCommand() {
    ////////////
    /// INFO ///
    ////////////
    @SubCommand(
        name = "info",
        description = "Everything you need to know about premium"
    )
    fun info(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {

    }

    //////////////////
    /// DEPRECATED ///
    //////////////////
    @SubCommand(
        name = "upgrade",
        description = "Upgrade a server to premium using the old subscription system",
        group = "deprecated",
        groupDescription = "0"
    )
    fun deprecatedUpgrade(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {

    }

    @SubCommand(
        name = "downgrade",
        description = "Downgrade a server from premium using the old subscription system",
        group = "deprecated",
        groupDescription = "0"
    )
    fun deprecatedDowngrade(
        event: SlashCommandInteractionEvent,
        ctx: SettingsInteractionContext
    ) {

    }
}