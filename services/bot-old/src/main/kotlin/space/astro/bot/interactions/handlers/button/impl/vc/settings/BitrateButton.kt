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
import space.astro.bot.interactions.handlers.modal.impl.vc.BitrateModal
import space.astro.bot.models.discord.vc.VCOperationCTX

@Button(
    id = InteractionIds.Button.VC_BITRATE,
    action = InteractionAction.VC_BITRATE
)
class BitrateButton(
    private val interactionComponentBuilder: InteractionComponentBuilder
) : space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.UNKNOWN
        )
        ctx: VcInteractionContext
    ) {
        val maxBitrate = ctx.vcOperationCTX.generatorData.commandsSettings.maxBitrate?.coerceAtMost(ctx.guild.maxBitrate) ?: ctx.guild.maxBitrate

        val textInput = TextInput.create(BitrateModal.BITRATE_TEXT_INPUT_ID, "Voice channel bitrate", TextInputStyle.SHORT)
            .setPlaceholder("64")
            .setMinLength(1)
            .setMaxLength(maxBitrate.toString().length)
            .build()

        val modal = interactionComponentBuilder.modalWithTextInput(
            id = InteractionIds.Modal.VC_BITRATE,
            title = "Voice channel editor",
            textInput = textInput
        )

        ctx.replyHandler.replyModal(modal)
    }
}