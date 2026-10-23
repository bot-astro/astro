package space.astro.bot.commands.vc.settings

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.ModalHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis

@Component
class BitrateModal(
    contextResolverFactory: VcContextResolverFactory,
) : ModalHandler<VcInteractionContext> {
    companion object {
        const val MODAL_ID = "modal/vc/bitrate"
        const val BITRATE_INPUT_ID = "bitrate"
    }

    override val id = MODAL_ID
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: ModalInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val bitrate = event.getValue(BITRATE_INPUT_ID)?.asString?.toIntOrNull()?.times(1000)
        val commandsSettings = ctx.vcOperationCTX.generatorSettings.commandsSettings
        val maxBitrate = commandsSettings.maxBitrate?.coerceAtMost(ctx.guild.maxBitrate) ?: ctx.guild.maxBitrate
        val minBitrate = (commandsSettings.minBitrate ?: 8000).coerceAtLeast(8000)

        if (bitrate == null || bitrate < minBitrate || bitrate > maxBitrate) {
            ctx.responder.error(
                "vc.bitrate.out_of_range",
                "min" to (minBitrate / 1000).toString(),
                "max" to (maxBitrate / 1000).toString(),
            )
            return
        }

        val vcCtx = ctx.vcOperationCTX
        vcCtx.temporaryVCManager.setBitrate(bitrate)
        vcCtx.markTemporaryVCManagerAsUpdated()
        vcCtx.queueUpdatedManagers()

        ctx.responder.reply(
            Containers.default(ctx.t("vc.bitrate.set", "emoji" to Emojis.bitrate.formatted, "bitrate" to (bitrate / 1000).toString())),
        )
    }
}
