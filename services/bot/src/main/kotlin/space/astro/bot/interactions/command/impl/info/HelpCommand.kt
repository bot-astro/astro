package space.astro.bot.interactions.command.impl.info

import dev.minn.jda.ktx.messages.Embed
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.command.AbstractCommand
import space.astro.bot.interactions.command.BaseCommand
import space.astro.bot.interactions.command.Command
import space.astro.bot.interactions.InteractionContext
import space.astro.bot.core.ui.Buttons
import space.astro.shared.core.util.extention.linkFromLink
import space.astro.shared.core.util.ui.Colors
import space.astro.shared.core.util.ui.Links


@Command(
    name = "help",
    description = "get started using Astro"
)
class HelpCommand : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext
    ) {
        event
            .replyEmbeds(buildHelpEmbed())
            .setActionRow(Buttons.Bundles.help)
            .queue()
    }

    private fun buildHelpEmbed(): MessageEmbed {
        return Embed(
            color = Colors.purple.rgb,
            authorName = "Help panel",
            authorUrl = Links.WEBSITE,
            authorIcon = Links.LOGO,
            description = "Astro can be used to generate temporary voice channels & connect voice channels to roles!" +
                    "\n\nYou can setup the bot or customise it on its ${Links.DASHBOARD.linkFromLink("dashboard")}" +
                    "\nUse the buttons below for other useful links!"
        )
    }
}