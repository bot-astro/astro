package space.astro.bot.interactions.button.impl.vc.permissions

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.button.AbstractButton
import space.astro.bot.interactions.button.Button
import space.astro.bot.interactions.button.ButtonRunnable
import space.astro.bot.interactions.command.VcInteractionContextInfo
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.models.database.VCState

@Button(
    id = InteractionIds.Button.VC_UNHIDE,
    action = InteractionAction.VC_UNHIDE
)
class UnhideButton(
    val vcPermissionManager: VCPermissionManager,
    val temporaryVCDao: TemporaryVCDao
) : AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        vcPermissionManager.changeState(ctx.vcOperationCTX, VCState.UNHIDDEN)
        ctx.vcOperationCTX.queueUpdatedManagers()
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        event.replyEmbeds(
            Embeds.default(
            "Your VC has been ${Emojis.unhide.formatted} un-hidden!"
        )).setEphemeral(true).queue()
    }
}