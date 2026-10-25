package space.astro.bot.commands.setup

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.BaseContextResolver
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.ButtonHandler

@Component
class SetupButton(
    private val setupManager: SetupManager,
) : ButtonHandler<InteractionContext> {
    companion object {
        const val ID = "button/settings/setup"
    }

    override val id = ID
    override val settings =
        InteractionSettings(
            ack = Ack.DEFER_REPLY_EPHEMERAL,
            memberPermissions = setOf(Permission.MANAGE_SERVER),
            botPermissions = setOf(Permission.MANAGE_CHANNEL, Permission.VIEW_CHANNEL),
        )
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: ButtonInteractionEvent,
        ctx: InteractionContext,
    ) = setupManager.setup(ctx)
}
