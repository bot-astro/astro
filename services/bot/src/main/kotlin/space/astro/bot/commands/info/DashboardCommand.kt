package space.astro.bot.commands.info

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.BaseContextResolver
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.ui.Embeds
import space.astro.bot.ui.Emojis
import space.astro.bot.ui.Links

@Component
class DashboardCommand : SlashHandler<InteractionContext> {
    override val data =
        Commands
            .slash("dashboard", "Points you to the dashboard where you can configure the bot settings for your server")
            .setContexts(InteractionContextType.GUILD)
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY)
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext,
    ) {
        ctx.responder.reply(
            Embeds.default(
                "Setup Astro and configure all of its settings on the ${Emojis.dashboard.formatted} [`Dashboard`](${Links.DASHBOARD})",
            ),
            listOf(ActionRow.of(Button.link(Links.DASHBOARD, "Dashboard").withEmoji(Emojis.dashboard))),
        )
    }
}
