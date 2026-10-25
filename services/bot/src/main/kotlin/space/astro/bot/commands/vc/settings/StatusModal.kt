package space.astro.bot.commands.vc.settings

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.ModalHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers

@Component
class StatusModal(
    contextResolverFactory: VcContextResolverFactory,
) : ModalHandler<VcInteractionContext> {
    companion object {
        const val MODAL_ID = "modal/vc/status"
        const val STATUS_INPUT_ID = "status"
    }

    override val id = MODAL_ID
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.UNKNOWN,
        )

    override suspend fun handle(
        event: ModalInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        // an empty input unsets the status
        val status =
            event
                .getValue(STATUS_INPUT_ID)
                ?.asString
                ?.trim()
                .orEmpty()

        ctx.vcOperationCTX.temporaryVC
            .modifyStatus(status)
            .reason(ctx.t("reason.vc.status_updated"))
            .await()

        val reply =
            if (status.isEmpty()) {
                ctx.t("vc.status.unset")
            } else {
                ctx.t("vc.status.set", "status" to status)
            }

        ctx.responder.reply(Containers.default(reply))
    }
}
