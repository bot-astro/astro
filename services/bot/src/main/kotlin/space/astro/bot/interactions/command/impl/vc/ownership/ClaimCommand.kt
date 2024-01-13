package space.astro.bot.interactions.command.impl.vc.ownership

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import space.astro.bot.components.managers.vc.VCOwnershipManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.util.extention.asRoleMention

@Command(
    name = "claim",
    description = "Ask to get the ownership of the voice channel so that you can manage it",
    category = CommandCategory.VC,
    action = InteractionAction.VC_CLAIM
)
class ClaimCommand(
    private val vcOwnershipManager: VCOwnershipManager,
    private val temporaryVCDao: TemporaryVCDao
) : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = false,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        /////////////////////
        /// ALREADY OWNER ///
        /////////////////////
        if (ctx.vcOperationCTX.temporaryVCData.ownerId == ctx.memberId) {
            event.replyEmbeds(Embeds.default("You are already the owner of the voice channel"))
                .setEphemeral(true)
                .queue()

            return
        }

        /////////////////////
        /// OWNER MISSING ///
        /////////////////////
        if (ctx.vcOperationCTX.temporaryVCOwner == null) {
            vcOwnershipManager.changeOwner(ctx.vcOperationCTX, ctx.member)
            ctx.vcOperationCTX.queueUpdatedManagers()
            temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)
            vcOwnershipManager.handleOwnerRoleMigration(ctx.vcOperationCTX, ctx.vcOperationCTX.temporaryVCOwner, ctx.member)

            event.replyEmbeds(Embeds.default("You are now the owner of the vc (the previous owner left)."))
                .setEphemeral(true)
                .queue()

            return
        }

        //////////////////////
        /// MODERATOR ROLE ///
        //////////////////////
        if (ctx.member.roles.any { it.id == ctx.vcOperationCTX.generatorData.ownerRole }) {
            vcOwnershipManager.changeOwner(ctx.vcOperationCTX, ctx.member)
            ctx.vcOperationCTX.queueUpdatedManagers()
            temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)
            vcOwnershipManager.handleOwnerRoleMigration(ctx.vcOperationCTX, ctx.vcOperationCTX.temporaryVCOwner, ctx.member)

            event.replyEmbeds(Embeds.default("You are now the owner of the vc." +
                    "\n\n*This operation was instant because you have the moderator role: ${ctx.vcOperationCTX.generatorData.ownerRole?.asRoleMention()}*"))
                .setEphemeral(true)
                .queue()

            return
        }

        /////////////////////////////////////////////////////
        /// SEND INSTRUCTION ON HOW TO TRANSFER OWNERSHIP ///
        /////////////////////////////////////////////////////
        event.replyEmbeds(Embeds.default("A claim request has been sent in ${ctx.vcOperationCTX.temporaryVC.asMention}"))
            .setEphemeral(true)
            .queue()

        ctx.vcOperationCTX.temporaryVC.sendMessage(
            MessageCreateBuilder()
                .setContent(ctx.vcOperationCTX.temporaryVCOwner?.asMention ?: "")
                .setEmbeds(Embeds.default("${ctx.member.asMention} asked for ownership of the voice channel." +
                        "\nTo accept his request run the following command: `/transfer user:@${ctx.member.effectiveName}`"))
                .build()
        ).queue()
    }
}