package space.astro.bot.commands.vc.permissions

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.interactions.Ack
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
            .slash("permit", "Permit a user or role to join your VC")
            .setContexts(InteractionContextType.GUILD)
            .addSubcommands(
                SubcommandData(USER_SUBCOMMAND, "Permit a user to join your VC")
                    .addOption(OptionType.USER, "user", "The user to permit in your channel", true),
                SubcommandData(ROLE_SUBCOMMAND, "Permit a role to join your VC")
                    .addOption(OptionType.ROLE, "role", "The role to permit in your channel", true),
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
                    ctx.responder.error("The user you provided is not in this server!")
                    return
                }

                vcPermissionManager.permit(vcCtx, memberIds = listOf(member.idLong), roles = emptyList())
                vcCtx.queueUpdatedManagers()

                ctx.responder.reply(Containers.default("${member.asMention} can now join your VC!"))
            }

            ROLE_SUBCOMMAND -> {
                val role = event.getOption("role")?.asRole

                if (role == null) {
                    ctx.responder.error("Please provide a valid role")
                    return
                }

                vcPermissionManager.permit(vcCtx, memberIds = emptyList(), roles = listOf(role))
                vcCtx.queueUpdatedManagers()

                ctx.responder.reply(Containers.default("${role.asMention} role can now join your temporary VC!"))
            }

            else -> ctx.responder.error("Unknown subcommand")
        }
    }
}
