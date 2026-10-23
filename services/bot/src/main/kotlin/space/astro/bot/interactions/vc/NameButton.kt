package space.astro.bot.interactions.vc

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
 * Opens [NameModal]
 */
@Component
class NameButton(
    contextResolverFactory: VcContextResolverFactory,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/name"
    override val settings = InteractionSettings(ack = Ack.NONE)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.USER_RENAME,
        )

    override suspend fun handle(
        event: ButtonInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val nameInput =
            TextInput
                .create(NameModal.NAME_INPUT_ID, TextInputStyle.SHORT)
                .setPlaceholder("Playing Rocket League")
                .setMinLength(2)
                .setMaxLength(200)
                .build()

        val modal =
            Modal
                .create(NameModal.MODAL_ID, "Voice channel editor")
                .addComponents(Label.of("Voice channel name", nameInput))
                .build()

        event.replyModal(modal).await()
    }
}
