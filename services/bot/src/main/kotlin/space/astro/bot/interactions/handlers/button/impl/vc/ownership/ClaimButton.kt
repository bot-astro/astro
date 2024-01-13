package space.astro.bot.interactions.handlers.button.impl.vc.ownership

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder
import space.astro.bot.components.managers.vc.VCOwnershipManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.button.Button
import space.astro.bot.interactions.handlers.button.ButtonRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.util.extention.asRoleMention

@Button(
    id = InteractionIds.Button.VC_CLAIM,
    action = InteractionAction.VC_CLAIM
)
class ClaimButton(
    private val vcOwnershipManager: VCOwnershipManager,
    private val temporaryVCDao: TemporaryVCDao
) : space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
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
            ctx.replyHandler.replyEmbed(Embeds.default("You are already the owner of the voice channel"))
            return
        }

        /////////////////////
        /// OWNER MISSING ///
        /////////////////////
        if (ctx.vcOperationCTX.temporaryVCOwner == null) {
            ctx.replyHandler.deferReply()

            vcOwnershipManager.changeOwner(ctx.vcOperationCTX, ctx.member)
            ctx.vcOperationCTX.queueUpdatedManagers()
            temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)
            vcOwnershipManager.handleOwnerRoleMigration(ctx.vcOperationCTX, ctx.vcOperationCTX.temporaryVCOwner, ctx.member)

            ctx.replyHandler.replyEmbed(Embeds.default("You are now the owner of the vc (the previous owner left)."))
            return
        }

        //////////////////////
        /// MODERATOR ROLE ///
        //////////////////////
        if (ctx.member.roles.any { it.id == ctx.vcOperationCTX.generatorData.ownerRole }) {
            ctx.replyHandler.deferReply()

            vcOwnershipManager.changeOwner(ctx.vcOperationCTX, ctx.member)
            ctx.vcOperationCTX.queueUpdatedManagers()
            temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)
            vcOwnershipManager.handleOwnerRoleMigration(ctx.vcOperationCTX, ctx.vcOperationCTX.temporaryVCOwner, ctx.member)

            ctx.replyHandler.replyEmbed(Embeds.default("You are now the owner of the vc." +
                    "\n\n*This operation was instant because you have the moderator role: ${ctx.vcOperationCTX.generatorData.ownerRole?.asRoleMention()}*"))
            return
        }

        /////////////////////////////////////////////////////
        /// SEND INSTRUCTION ON HOW TO TRANSFER OWNERSHIP ///
        /////////////////////////////////////////////////////
        ctx.replyHandler.replyEmbed(Embeds.default("A claim request has been sent in ${ctx.vcOperationCTX.temporaryVC.asMention}"))

        ctx.vcOperationCTX.temporaryVC.sendMessage(
            MessageCreateBuilder()
                .setContent(ctx.vcOperationCTX.temporaryVCOwner?.asMention ?: "")
                .setEmbeds(Embeds.default("${ctx.member.asMention} asked for ownership of the voice channel." +
                        "\nTo accept his request run the following command: `/transfer user:@${ctx.member.effectiveName}`"))
                .build()
        ).queue()
    }
}