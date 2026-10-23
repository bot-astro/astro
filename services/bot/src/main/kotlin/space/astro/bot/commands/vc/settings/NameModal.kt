package space.astro.bot.commands.vc.settings

import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCNameManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.ModalHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers
import space.astro.bot.ui.Emojis

@Component
class NameModal(
    contextResolverFactory: VcContextResolverFactory,
    private val vcNameManager: VCNameManager,
) : ModalHandler<VcInteractionContext> {
    companion object {
        const val MODAL_ID = "modal/vc/name"
        const val NAME_INPUT_ID = "name"
    }

    override val id = MODAL_ID
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.USER_RENAME,
        )

    override suspend fun handle(
        event: ModalInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val name = event.getValue(NAME_INPUT_ID)?.asString

        if (name.isNullOrBlank()) {
            ctx.responder.error("vc.name.invalid")
            return
        }

        vcNameManager.performVCRename(ctx.vcOperationCTX, name)
        ctx.vcOperationCTX.queueUpdatedManagers()

        ctx.responder.reply(Containers.default(ctx.t("vc.name.set", "emoji" to Emojis.name.formatted, "name" to name)))
    }
}
