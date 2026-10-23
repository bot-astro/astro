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
            ctx.responder.error("vc.ban.none_banned")
            return
        }

        vcCtx.queueUpdatedManagers()

        ctx.responder.reply(
            Containers.default(
                ctx.t("vc.ban.banned_many", "mentions" to banned.joinToString(", ") { it.asMention }),
            ),
        )
    }
}
