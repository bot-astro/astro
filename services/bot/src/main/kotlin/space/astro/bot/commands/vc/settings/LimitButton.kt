package space.astro.bot.commands.vc.settings

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.modals.Modal
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.interactions.handlers.ButtonHandler

/**
 * Opens [LimitModal]
 */
@Component
class LimitButton(
    contextResolverFactory: VcContextResolverFactory,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/limit"
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
        val limitInput =
            TextInput
                .create(LimitModal.LIMIT_INPUT_ID, TextInputStyle.SHORT)
                .setPlaceholder("4")
                .setMinLength(1)
                .setMaxLength(2)
                .build()

        val modal =
            Modal
                .create(LimitModal.MODAL_ID, "Voice channel editor")
                .addComponents(Label.of("Voice channel limit", limitInput))
                .build()

        event.replyModal(modal).await()
    }
}
