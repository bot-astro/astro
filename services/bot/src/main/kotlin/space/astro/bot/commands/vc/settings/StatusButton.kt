package space.astro.bot.commands.vc.settings

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.channel.attribute.IVoiceStatusChannel
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.modals.Modal
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.models.discord.vc.VCOperationCTX

/**
 * Opens [StatusModal]
 */
@Component
class StatusButton(
    contextResolverFactory: VcContextResolverFactory,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/status"
    override val settings = InteractionSettings(ack = Ack.NONE)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.UNKNOWN,
        )

    override suspend fun handle(
        event: ButtonInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val statusInput =
            TextInput
                .create(StatusModal.STATUS_INPUT_ID, TextInputStyle.SHORT)
                .setPlaceholder(ctx.t("vc.status.placeholder"))
                .setValue(
                    ctx.vcOperationCTX.temporaryVC.status
                        .ifEmpty { null },
                ).setRequired(false)
                .setMaxLength(IVoiceStatusChannel.MAX_STATUS_LENGTH)
                .build()

        val modal =
            Modal
                .create(StatusModal.MODAL_ID, ctx.t("vc.modal.title"))
                .addComponents(Label.of(ctx.t("vc.status.label"), statusInput))
                .build()

        event.replyModal(modal).await()
    }
}
