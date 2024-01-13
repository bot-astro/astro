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
import space.astro.bot.interactions.handlers.modal.impl.vc.NameModal
import space.astro.bot.models.discord.vc.VCOperationCTX

@Button(
    id = InteractionIds.Button.VC_NAME,
    action = InteractionAction.VC_NAME
)
class NameButton(
    private val interactionComponentBuilder: InteractionComponentBuilder
) : space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.USER_RENAME
        )
        ctx: VcInteractionContext,
    ) {
        val textInput = TextInput.create(NameModal.NAME_TEXT_INPUT_ID, "Voice channel name", TextInputStyle.SHORT)
            .setPlaceholder("Playing Rocket League")
            .setMinLength(2)
            .setMaxLength(200)
            .build()

        val modal = interactionComponentBuilder.modalWithTextInput(
            id = InteractionIds.Modal.VC_NAME,
            title = "Voice channel editor",
            textInput = textInput
        )

        ctx.replyHandler.replyModal(modal)
    }
}