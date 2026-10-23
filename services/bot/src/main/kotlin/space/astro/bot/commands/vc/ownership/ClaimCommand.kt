package space.astro.bot.commands.vc.ownership

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
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
import space.astro.shared.core.utils.extensions.asRoleMention
import space.astro.bot.interactions.handlers.SlashHandler

@Component
class ClaimCommand(
    contextResolverFactory: VcContextResolverFactory,
    private val vcOwnershipManager: VCOwnershipManager,
    private val temporaryVCRepository: TemporaryVCRepository,
) : SlashHandler<VcInteractionContext> {
    override val data =
        Commands
            .slash("claim", "Ask to get the ownership of the voice channel so that you can manage it")
            .setContexts(InteractionContextType.GUILD)
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = false,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val vcCtx = ctx.vcOperationCTX

        // already owner
        if (vcCtx.temporaryVCEntity.ownerID == ctx.memberId) {
            ctx.responder.reply(Containers.default("You are already the owner of the voice channel"))
            return
        }

        // owner missing
        if (vcCtx.temporaryVCOwner == null) {
            vcOwnershipManager.changeOwner(vcCtx, ctx.member)
            vcCtx.queueUpdatedManagers()
            withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, vcCtx.temporaryVCEntity) }
            vcOwnershipManager.handleOwnerRoleMigration(vcCtx, null, ctx.member)

            ctx.responder.reply(Containers.default("You are now the owner of the vc (the previous owner left)."))
            return
        }

        // moderator role
        val immuneRoleId = vcCtx.generatorSettings.permissionsImmuneRole
        if (immuneRoleId != null && ctx.member.roles.any { it.id == immuneRoleId }) {
            val oldOwner = vcCtx.temporaryVCOwner

            vcOwnershipManager.changeOwner(vcCtx, ctx.member)
            vcCtx.queueUpdatedManagers()
            withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, vcCtx.temporaryVCEntity) }
            vcOwnershipManager.handleOwnerRoleMigration(vcCtx, oldOwner, ctx.member)

            ctx.responder.reply(
                Containers.default(
                    "You are now the owner of the vc." +
                        "\n\n*This operation was instant because you have the moderator role: ${immuneRoleId.asRoleMention()}*",
                ),
            )
            return
        }

        // send instructions on how to transfer the ownership
        ctx.responder.reply(Containers.default("A claim request has been sent in ${vcCtx.temporaryVC.asMention}"))

        val ownerMention = vcCtx.temporaryVCOwner?.asMention ?: return

        vcCtx.temporaryVC
            .sendMessage(
                Containers.message(
                    // mention outside of the container so the owner gets pinged
                    Containers.text(ownerMention),
                    Containers.default(
                        "${ctx.member.asMention} asked for ownership of the voice channel." +
                            "\nTo accept their request run the following command: `/transfer user:@${ctx.member.effectiveName}`",
                    ),
                ),
            ).queue()
    }
}
