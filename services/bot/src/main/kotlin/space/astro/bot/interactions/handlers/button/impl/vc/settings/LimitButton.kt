package space.astro.bot.interactions.handlers.button.impl.vc.settings

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.interactions.components.text.TextInput
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionComponentBuilder
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.button.Button
import space.astro.bot.interactions.handlers.button.ButtonRunnable
import space.astro.bot.interactions.handlers.modal.impl.vc.LimitModal
import space.astro.bot.models.discord.vc.VCOperationCTX

@Button(
    id = InteractionIds.Button.VC_LIMIT,
    action = InteractionAction.VC_LIMIT
)
class LimitButton(
    private val interactionComponentBuilder: InteractionComponentBuilder
) : space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.UNKNOWN
        )
        ctx: VcInteractionContext,
    ) {
        val textInput = TextInput.create(LimitModal.LIMIT_TEXT_INPUT_ID, "Voice channel limit", TextInputStyle.SHORT)
            .setPlaceholder("4")
            .setMinLength(1)
            .setMaxLength(2)
            .build()

        val modal = interactionComponentBuilder.modalWithTextInput(
            id = InteractionIds.Modal.VC_LIMIT,
            title = "Voice channel editor",
            textInput = textInput
        )

        ctx.replyHandler.replyModal(modal)
    }
}