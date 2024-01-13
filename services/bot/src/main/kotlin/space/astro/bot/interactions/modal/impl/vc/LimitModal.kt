package space.astro.bot.interactions.modal.impl.vc

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.button.Button
import space.astro.bot.interactions.command.VcInteractionContextInfo
import space.astro.bot.interactions.modal.AbstractModal
import space.astro.bot.interactions.modal.ModalRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX

@Button(
    id = InteractionIds.Modal.VC_LIMIT,
    action = InteractionAction.VC_LIMIT
)
class LimitModal : AbstractModal() {

    companion object {
        const val LIMIT_TEXT_INPUT_ID = "limit"
    }

    @ModalRunnable
    fun run(
        event: ModalInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        val limit = event.getValue(LIMIT_TEXT_INPUT_ID)?.asString?.toIntOrNull()?.times(1000)
        val maxUserLimit = ctx.vcOperationCTX.generatorData.commandsSettings.maxUserLimit
        val minUserLimit = ctx.vcOperationCTX.generatorData.commandsSettings.minUserLimit

        if (limit == null || limit < minUserLimit || limit > maxUserLimit) {
            event.replyEmbeds(
                Embeds.error(
                "User limit must be between `$minUserLimit` and `$maxUserLimit`." +
                        "\n(*Those limits were set by the moderators of the server*)",
            )).setEphemeral(true).queue()
            return
        }
        ctx.vcOperationCTX.temporaryVCManager.setUserLimit(limit).queue()

        event.replyEmbeds(
            Embeds.default(
            "${Emojis.limit.formatted} User limit set to $limit users!"
        ))
    }
}