package space.astro.bot.commands.vc.permissions

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.CommandLocalization
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers

@Component
class InviteCommand(
    contextResolverFactory: VcContextResolverFactory,
    private val vcPermissionManager: VCPermissionManager,
) : SlashHandler<VcInteractionContext> {
    companion object {
        private val USER_OPTIONS = listOf("user", "second-user", "third-user")
    }

    override val data =
        Commands
            .slash("invite", CommandLocalization.description("invite"))
            .setContexts(InteractionContextType.GUILD)
            .addOption(OptionType.USER, USER_OPTIONS[0], CommandLocalization.description("invite.user"), true)
            .addOption(OptionType.USER, USER_OPTIONS[1], CommandLocalization.description("invite.second-user"), false)
            .addOption(OptionType.USER, USER_OPTIONS[2], CommandLocalization.description("invite.third-user"), false)
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.UNKNOWN,
        )

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val members =
            USER_OPTIONS
                .mapNotNull { event.getOption(it)?.asMember }
                .filter { !it.user.isBot }
                .distinct()

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
                ?: vcCtx.temporaryVC
                    .createInvite()
                    .reason(ctx.t("reason.vc.invite_created"))
                    .await()

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
