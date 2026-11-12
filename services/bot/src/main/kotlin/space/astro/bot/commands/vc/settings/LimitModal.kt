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
class LimitModal(
    contextResolverFactory: VcContextResolverFactory,
) : ModalHandler<VcInteractionContext> {
    companion object {
        const val MODAL_ID = "modal/vc/limit"
        const val LIMIT_INPUT_ID = "limit"
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
        val limit = event.getValue(LIMIT_INPUT_ID)?.asString?.toIntOrNull()
        val commandsSettings = ctx.vcOperationCTX.generatorSettings.commandsSettings
        val minUserLimit = commandsSettings.minUserLimit ?: 0
        val maxUserLimit = commandsSettings.maxUserLimit ?: 99

        if (limit == null || limit < minUserLimit || limit > maxUserLimit) {
            ctx.responder.error(
                "vc.limit.out_of_range",
                "min" to minUserLimit.toString(),
                "max" to maxUserLimit.toString(),
            )
            return
        }

        val vcCtx = ctx.vcOperationCTX
        vcCtx.temporaryVCManager.setUserLimit(limit)
        vcCtx.markTemporaryVCManagerAsUpdated()
        vcCtx.queueUpdatedManagers()

        ctx.responder.reply(Containers.default(ctx.t("vc.limit.set", "emoji" to Emojis.limit.formatted, "limit" to limit.toString())))
    }
}
