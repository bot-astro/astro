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
class BanMenu(
    contextResolverFactory: VcContextResolverFactory,
    private val vcPermissionManager: VCPermissionManager,
) : EntitySelectHandler<VcInteractionContext> {
    companion object {
        const val MENU_ID = "menu/vc/ban"
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
        val vcCtx = ctx.vcOperationCTX

        val banned =
            vcPermissionManager.kickAndBanMembersAndRoles(
                ctx = vcCtx,
                members = event.mentions.members,
                roles = event.mentions.roles,
            )

        if (banned.isEmpty()) {
            ctx.responder.error(
                "No users or roles were banned, this may be because they involve a role which is immune to this command!",
            )
            return
        }

        vcCtx.queueUpdatedManagers()

        ctx.responder.reply(
            Containers.default(
                "The following users and roles have been banned from your voice channel:" +
                    "\n${banned.joinToString(", ") { it.asMention }}",
            ),
        )
    }
}
