package space.astro.bot.interactions.handlers.button.impl.vc.permissions

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.button.Button
import space.astro.bot.interactions.handlers.button.ButtonRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.models.database.VCState

@Button(
    id = InteractionIds.Button.VC_LOCK,
    action = InteractionAction.VC_LOCK
)
class LockButton(
    val vcPermissionManager: VCPermissionManager,
    val temporaryVCDao: TemporaryVCDao
) : space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        ctx.replyHandler.deferReply()

        vcPermissionManager.changeState(ctx.vcOperationCTX, VCState.LOCKED)
        ctx.vcOperationCTX.queueUpdatedManagers()
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        ctx.replyHandler.replyEmbed(Embeds.default(
            "Your VC has been ${Emojis.lock.formatted} locked!"
        ))
    }
}