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
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.models.discord.vc.VCOperationCTX

/**
 * Opens [BitrateModal]
 */
@Component
class BitrateButton(
    contextResolverFactory: VcContextResolverFactory,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/bitrate"
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
        val commandsSettings = ctx.vcOperationCTX.generatorSettings.commandsSettings
        val maxBitrate = commandsSettings.maxBitrate?.coerceAtMost(ctx.guild.maxBitrate) ?: ctx.guild.maxBitrate

        val bitrateInput =
            TextInput
                .create(BitrateModal.BITRATE_INPUT_ID, TextInputStyle.SHORT)
                .setPlaceholder("64")
                .setMinLength(1)
                .setMaxLength(maxBitrate.toString().length)
                .build()

        val modal =
            Modal
                .create(BitrateModal.MODAL_ID, ctx.t("vc.modal.title"))
                .addComponents(Label.of(ctx.t("vc.bitrate.label"), bitrateInput))
                .build()

        event.replyModal(modal).await()
    }
}
