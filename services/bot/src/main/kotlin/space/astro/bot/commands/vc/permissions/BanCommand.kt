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
import space.astro.bot.ui.Emojis

@Component
class BanCommand(
    contextResolverFactory: VcContextResolverFactory,
    private val vcPermissionManager: VCPermissionManager,
) : SlashHandler<VcInteractionContext> {
    companion object {
        private const val USER_SUBCOMMAND = "user"
        private const val ROLE_SUBCOMMAND = "role"
    }

    override val data =
        Commands
            .slash("ban", CommandLocalization.description("ban"))
            .setContexts(InteractionContextType.GUILD)
            .addSubcommands(
                SubcommandData(USER_SUBCOMMAND, CommandLocalization.description("ban.user"))
                    .addOption(OptionType.USER, "user", CommandLocalization.description("ban.user.user"), true),
                SubcommandData(ROLE_SUBCOMMAND, CommandLocalization.description("ban.role"))
                    .addOption(OptionType.ROLE, "role", CommandLocalization.description("ban.role.role"), true),
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

                vcPermissionManager.kickAndBanMember(vcCtx, member)
                vcCtx.queueUpdatedManagers()

                ctx.responder.reply(Containers.default(ctx.t("vc.ban.banned_user", "user" to member.asMention)))
            }

            ROLE_SUBCOMMAND -> {
                val role = event.getOption("role")?.asRole

                if (role == null) {
                    ctx.responder.error("vc.permissions.invalid_role")
                    return
                }

                vcPermissionManager.banRole(vcCtx, role)
                vcCtx.queueUpdatedManagers()

                ctx.responder.reply(
                    Containers.default(ctx.t("vc.ban.banned_role", "role" to role.asMention, "emoji" to Emojis.ban.formatted)),
                )
            }

            else -> ctx.responder.error("vc.permissions.unknown_subcommand")
        }
    }
}
