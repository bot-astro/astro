package space.astro.bot.commands.vc.permissions

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
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
class PermitCommand(
    contextResolverFactory: VcContextResolverFactory,
    private val vcPermissionManager: VCPermissionManager,
) : SlashHandler<VcInteractionContext> {
    companion object {
        private const val USER_SUBCOMMAND = "user"
        private const val ROLE_SUBCOMMAND = "role"
    }

    override val data =
        Commands
            .slash("permit", CommandLocalization.description("permit"))
            .setContexts(InteractionContextType.GUILD)
            .addSubcommands(
                SubcommandData(USER_SUBCOMMAND, CommandLocalization.description("permit.user"))
                    .addOption(OptionType.USER, "user", CommandLocalization.description("permit.user.user"), true),
                SubcommandData(ROLE_SUBCOMMAND, CommandLocalization.description("permit.role"))
                    .addOption(OptionType.ROLE, "role", CommandLocalization.description("permit.role.role"), true),
            )
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
        val vcCtx = ctx.vcOperationCTX

        when (event.subcommandName) {
            USER_SUBCOMMAND -> {
                val member = event.getOption("user")?.asMember

                if (member == null) {
                    ctx.responder.error("vc.permissions.user_not_in_server")
                    return
                }

                vcPermissionManager.permit(vcCtx, memberIds = listOf(member.idLong), roles = emptyList())
                vcCtx.queueUpdatedManagers()

                ctx.responder.reply(Containers.default(ctx.t("vc.permit.permitted_user", "user" to member.asMention)))
            }

            ROLE_SUBCOMMAND -> {
                val role = event.getOption("role")?.asRole

                if (role == null) {
                    ctx.responder.error("vc.permissions.invalid_role")
                    return
                }

                vcPermissionManager.permit(vcCtx, memberIds = emptyList(), roles = listOf(role))
                vcCtx.queueUpdatedManagers()

                ctx.responder.reply(Containers.default(ctx.t("vc.permit.permitted_role", "role" to role.asMention)))
            }

            else -> ctx.responder.error("vc.permissions.unknown_subcommand")
        }
    }
}
