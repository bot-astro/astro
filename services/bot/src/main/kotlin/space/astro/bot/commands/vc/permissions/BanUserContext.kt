package space.astro.bot.commands.vc.permissions

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
class BanUserContext(
    contextResolverFactory: VcContextResolverFactory,
    private val vcPermissionManager: VCPermissionManager,
) : UserContextHandler<VcInteractionContext> {
    override val data =
        Commands
            .user("Ban from VC")
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
        val member = event.targetMember

        if (member == null) {
            ctx.responder.error("vc.permissions.user_not_in_server")
            return
        }

        val vcCtx = ctx.vcOperationCTX

        vcPermissionManager.kickAndBanMember(vcCtx, member)
        vcCtx.queueUpdatedManagers()

        ctx.responder.reply(Containers.default(ctx.t("vc.ban.banned_user", "user" to member.asMention)))
    }
}
