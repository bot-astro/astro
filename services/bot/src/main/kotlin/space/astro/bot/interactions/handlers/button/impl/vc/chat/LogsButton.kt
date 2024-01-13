package space.astro.bot.interactions.handlers.button.impl.vc.chat

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.button.Button
import space.astro.bot.interactions.handlers.button.ButtonRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.util.extention.asEnabledOrDisabled

@Button(
    id = InteractionIds.Button.VC_LOGS,
    action = InteractionAction.VC_LOGS
)
class LogsButton(
    private val temporaryVCDao: TemporaryVCDao
): space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        ctx.vcOperationCTX.temporaryVCData.chatLogs = !ctx.vcOperationCTX.temporaryVCData.chatLogs
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        ctx.replyHandler.replyEmbed(Embeds.default("Logs are now ${ctx.vcOperationCTX.temporaryVCData.chatLogs.asEnabledOrDisabled()}"))
    }
}