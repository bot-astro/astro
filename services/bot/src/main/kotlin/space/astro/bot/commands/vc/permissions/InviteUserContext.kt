package space.astro.bot.commands.vc.permissions

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.UserContextHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers

@Component
class InviteUserContext(
    contextResolverFactory: VcContextResolverFactory,
    private val vcPermissionManager: VCPermissionManager,
) : UserContextHandler<VcInteractionContext> {
    override val data =
        Commands
            .user("Invite to VC")
            .setContexts(InteractionContextType.GUILD)
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.UNKNOWN,
        )

    override suspend fun handle(
        event: UserContextInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val member = event.targetMember?.takeIf { !it.user.isBot }

        if (member == null) {
            ctx.responder.error("vc.invite.no_valid_users")
            return
        }

        val vcCtx = ctx.vcOperationCTX

        val vcInvite =
            vcCtx.temporaryVC
                .retrieveInvites()
                .await()
                .firstOrNull()
                ?: vcCtx.temporaryVC
                    .createInvite()
                    .reason(ctx.t("reason.vc.invite_created"))
                    .await()

        vcPermissionManager.permitMembers(vcCtx, listOf(member))
        vcCtx.queueUpdatedManagers()

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

        ctx.responder.reply(Containers.default(ctx.t("vc.invite.invited", "users" to member.asMention)))
    }
}
