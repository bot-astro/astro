package space.astro.bot.commands.vc.permissions

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis
import space.astro.shared.core.models.database.TemporaryVCEntity.TemporaryVCState

/**
 * `/unhide` and the unhide button
 */
@Component
class UnhideInteraction(
    contextResolverFactory: VcContextResolverFactory,
    private val vcPermissionManager: VCPermissionManager,
) : SlashHandler<VcInteractionContext>,
    ButtonHandler<VcInteractionContext> {
    companion object {
        const val BUTTON_ID = "button/vc/unhide"
    }

    override val id = BUTTON_ID
    override val data = Commands.slash("unhide", "Un-hide your voice channel making it visible to everyone").setContexts(InteractionContextType.GUILD)
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: VcInteractionContext,
    ) = unhide(ctx)

    override suspend fun handle(
        event: ButtonInteractionEvent,
        ctx: VcInteractionContext,
    ) = unhide(ctx)

    private suspend fun unhide(ctx: VcInteractionContext) {
        vcPermissionManager.changeState(ctx.vcOperationCTX, TemporaryVCState.UNHIDDEN)
        ctx.vcOperationCTX.queueUpdatedManagers()

        ctx.responder.reply(Containers.default("Your VC has been ${Emojis.unhide.formatted} un-hidden!"))
    }
}
