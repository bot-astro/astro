package space.astro.bot.commands.vc.ownership

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCOwnershipManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers
import space.astro.shared.core.repositories.redis.TemporaryVCRepository
import space.astro.bot.interactions.handlers.SlashHandler

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
            .slash("transfer", "Transfer the ownership of your VC to someone else")
            .setContexts(InteractionContextType.GUILD)
            .addOption(OptionType.USER, USER_OPTION, "The user that will get ownership of the VC", true)
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
            ctx.responder.error("The user you provided is not in this server!")
            return
        }

        if (member.user.isBot) {
            ctx.responder.error("Cannot transfer the ownership to a bot")
            return
        }

        val vcCtx = ctx.vcOperationCTX

        if (member.voiceState?.channel?.id != vcCtx.temporaryVC.id) {
            ctx.responder.error("The user you provided is not in your voice channel!")
            return
        }

        vcOwnershipManager.changeOwner(vcCtx, member)
        vcCtx.queueUpdatedManagers()
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, vcCtx.temporaryVCEntity) }
        vcOwnershipManager.handleOwnerRoleMigration(vcCtx, ctx.member, member)

        ctx.responder.reply(Containers.default("Ownership transferred to ${member.asMention}"))
    }
}
