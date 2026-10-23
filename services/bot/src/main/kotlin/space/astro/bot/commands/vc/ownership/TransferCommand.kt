package space.astro.bot.commands.vc.ownership

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCOwnershipManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.CommandLocalization
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.SlashHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

@Component
class TransferCommand(
    contextResolverFactory: VcContextResolverFactory,
    private val vcOwnershipManager: VCOwnershipManager,
    private val temporaryVCRepository: TemporaryVCRepository,
) : SlashHandler<VcInteractionContext> {
    companion object {
        private const val USER_OPTION = "user"
    }

    override val data =
        Commands
            .slash("transfer", CommandLocalization.description("transfer"))
            .setContexts(InteractionContextType.GUILD)
            .addOption(OptionType.USER, USER_OPTION, CommandLocalization.description("transfer.user"), true)
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val member = event.getOption(USER_OPTION)?.asMember

        if (member == null) {
            ctx.responder.error("vc.permissions.user_not_in_server")
            return
        }

        if (member.user.isBot) {
            ctx.responder.error("vc.transfer.bot")
            return
        }

        val vcCtx = ctx.vcOperationCTX

        if (member.voiceState?.channel?.id != vcCtx.temporaryVC.id) {
            ctx.responder.error("vc.transfer.not_in_vc")
            return
        }

        vcOwnershipManager.changeOwner(vcCtx, member)
        vcCtx.queueUpdatedManagers()
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, vcCtx.temporaryVCEntity) }
        vcOwnershipManager.handleOwnerRoleMigration(vcCtx, ctx.member, member)

        ctx.responder.reply(Containers.default(ctx.t("vc.transfer.done", "user" to member.asMention)))
    }
}
