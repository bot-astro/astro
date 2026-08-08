package space.astro.bot.interactions.handlers.modal.impl.vc

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.modal.AbstractModal
import space.astro.bot.interactions.handlers.modal.Modal
import space.astro.bot.interactions.handlers.modal.ModalRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX

@Modal(
    id = InteractionIds.Modal.VC_BITRATE,
    action = InteractionAction.VC_BITRATE
)
class BitrateModal : AbstractModal() {

    companion object {
        const val BITRATE_TEXT_INPUT_ID = "bitrate"
    }

    @ModalRunnable
    suspend fun run(
        event: ModalInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        val calculatedBitrate = event.getValue(BITRATE_TEXT_INPUT_ID)?.asString?.toIntOrNull()?.times(1000)
        val maxBitrate = ctx.vcOperationCTX.generatorData.commandsSettings.maxBitrate?.coerceAtMost(ctx.guild.maxBitrate) ?: ctx.guild.maxBitrate
        val minBitrate = ctx.vcOperationCTX.generatorData.commandsSettings.minBitrate.coerceAtLeast(8000)

        if (calculatedBitrate == null || calculatedBitrate < minBitrate || calculatedBitrate > maxBitrate) {
            ctx.replyHandler.replyEmbed(
                Embeds.error(
                "The bitrate must be between `${minBitrate / 1000} kbps` and `${maxBitrate / 1000} kbps`!" +
                        "\n(*Those bounds were set by the moderators of the server*)"
            ))
            return
        }

        ctx.vcOperationCTX.temporaryVCManager.setBitrate(calculatedBitrate).queue()

        ctx.replyHandler.replyEmbed(
            Embeds.default(
            "${Emojis.bitrate.formatted} Bitrate set to ${calculatedBitrate / 1000}kbps"
        ))
    }
}