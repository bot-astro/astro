package space.astro.bot.commands.setup

import net.dv8tion.jda.api.Permission
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

@Component
class SetupCommand(
    private val setupManager: SetupManager,
) : SlashHandler<InteractionContext> {
    override val data =
        Commands
            .slash("setup", CommandLocalization.description("setup"))
            .setContexts(InteractionContextType.GUILD)
    override val settings =
        InteractionSettings(
            ack = Ack.DEFER_REPLY_EPHEMERAL,
            memberPermissions = setOf(Permission.MANAGE_SERVER),
            botPermissions = setOf(Permission.MANAGE_CHANNEL, Permission.VIEW_CHANNEL),
        )
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: InteractionContext,
    ) = setupManager.setup(ctx)
}
