package space.astro.bot.commands.developer

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.interactions.CommandLocalization
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.BaseContextResolver
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.ui.Containers

/**
 * Dummy developer command, copy it to add new ones: the `developerOnly` setting is all that makes it a developer command
 */
@Component
class DevPingCommand : SlashHandler<InteractionContext> {
    override val data =
        Commands
            .slash("dev-ping", CommandLocalization.description("dev-ping"))
            .setContexts(InteractionContextType.GUILD)
    override val settings = InteractionSettings(developerOnly = true)
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext,
    ) {
        ctx.responder.reply(
            Containers.default(
                ctx.t("dev.ping.reply", "ping" to event.jda.gatewayPing, "shard" to event.jda.shardInfo.shardId),
            ),
        )
    }
}
