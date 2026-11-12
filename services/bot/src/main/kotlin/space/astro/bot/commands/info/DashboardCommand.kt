package space.astro.bot.commands.info

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.CommandLocalization
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.BaseContextResolver
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.ui.Buttons
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis
import space.astro.bot.ui.Links

@Component
class DashboardCommand : SlashHandler<InteractionContext> {
    override val data =
        Commands
            .slash("dashboard", CommandLocalization.description("dashboard"))
            .setContexts(InteractionContextType.GUILD)
    override val settings = InteractionSettings(ack = Ack.NONE)
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext,
    ) {
        ctx.responder.reply(
            Containers.default(
                ctx.t("dashboard.reply", "emoji" to Emojis.dashboard.formatted, "link" to Links.DASHBOARD),
            ),
            ActionRow.of(Buttons.dashboard(ctx.locale)),
            ephemeral = false,
        )
    }
}
