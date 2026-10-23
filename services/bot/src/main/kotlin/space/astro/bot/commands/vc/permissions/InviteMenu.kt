package space.astro.bot.commands.vc.permissions

import dev.minn.jda.ktx.coroutines.await
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
class InviteMenu(
    contextResolverFactory: VcContextResolverFactory,
    private val vcPermissionManager: VCPermissionManager,
) : EntitySelectHandler<VcInteractionContext> {
    companion object {
        const val MENU_ID = "menu/vc/invite"
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
        val members = event.mentions.members.filter { !it.user.isBot }

        if (members.isEmpty()) {
            ctx.responder.error("vc.invite.no_valid_users")
            return
        }

        val vcCtx = ctx.vcOperationCTX

        val vcInvite =
            vcCtx.temporaryVC
                .retrieveInvites()
                .await()
                .firstOrNull()
                ?: vcCtx.temporaryVC.createInvite().await()

        vcPermissionManager.permitMembers(vcCtx, members)
        vcCtx.queueUpdatedManagers()

        members.forEach { member ->
            member.user.openPrivateChannel().queue({ privateChannel ->
                privateChannel
                    .sendMessageComponents(
                        Containers.default(
                            ctx.t(
                                "vc.invite.dm",
                                "inviter" to ctx.member.asMention,
                                "guild" to ctx.guild.name,
                                "invite" to vcInvite.url,
                            ),
                        ),
                    ).queue(null) { }
            }) { }
        }

        ctx.responder.reply(Containers.default(ctx.t("vc.invite.invited", "users" to members.joinToString(", ") { it.asMention })))
    }
}
