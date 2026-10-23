package space.astro.bot.commands.vc.permissions

import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.EntitySelectHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers

@Component
class PermitMenu(
    contextResolverFactory: VcContextResolverFactory,
    private val vcPermissionManager: VCPermissionManager,
) : EntitySelectHandler<VcInteractionContext> {
    companion object {
        const val MENU_ID = "menu/vc/permit"
    }

    override val id = MENU_ID
    override val settings = InteractionSettings(ack = Ack.DEFER_EDIT)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: EntitySelectInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val members = event.mentions.members
        val roles = event.mentions.roles

        if (members.isEmpty() && roles.isEmpty()) {
            ctx.responder.error("You didn't provide valid users / roles")
            return
        }

        val vcCtx = ctx.vcOperationCTX

        vcPermissionManager.permit(vcCtx, memberIds = members.map { it.idLong }, roles = roles)
        vcCtx.queueUpdatedManagers()

        val permitted = members.map { it.asMention } + roles.map { it.asMention }

        ctx.responder.reply(
            Containers.default(
                "The following users and roles have been permitted in your voice channel:" +
                    "\n${permitted.joinToString(", ")}",
            ),
        )
    }
}
