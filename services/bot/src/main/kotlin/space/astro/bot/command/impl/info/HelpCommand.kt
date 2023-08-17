package space.astro.bot.command.impl.info

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.command.AbstractCommand
import space.astro.bot.command.BaseCommand
import space.astro.bot.command.Command
import space.astro.bot.command.CommandContext
import space.astro.bot.util.Buttons
import space.astro.bot.util.Emojis
import space.astro.shared.core.util.Links


@Command(
    name = "help",
    description = "get started using Astro"
)
class HelpCommand() : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        ctx: CommandContext
    ) {
        event
            .replyEmbeds(buildHelpEmbed())
            .setActionRow(Buttons.Bundles.help)
            .queue()
    }

    private fun buildHelpEmbed(): MessageEmbed {
        return Embed(
            authorName = "Help panel",
            authorUrl = Links.base,
            authorIcon = Links.logo,
            description = "Astro can be used to generate temporary voice channels & assign temporary roles to users in voice channels!" +
                    "\n\nYou can setup the bot or customise it on its [dashboard](${Links.dashboard})" +
                    "\nUse the buttons below for other useful links!"
        )
    }
}