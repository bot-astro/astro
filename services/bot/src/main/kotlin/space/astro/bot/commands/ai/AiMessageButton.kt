package space.astro.bot.commands.ai

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.modals.Modal
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.BaseContextResolver
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.ButtonHandler

/**
 * Opens [AiMessageModal]
 */
@Component
class AiMessageButton : ButtonHandler<InteractionContext> {
    companion object {
        const val ID = "button/ai/message"
    }

    override val id = ID
    override val settings =
        InteractionSettings(
            ack = Ack.NONE,
            memberPermissions = setOf(Permission.MANAGE_SERVER),
        )
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: ButtonInteractionEvent,
        ctx: InteractionContext,
    ) {
        val messageInput =
            TextInput
                .create(AiMessageModal.MESSAGE_INPUT_ID, TextInputStyle.PARAGRAPH)
                .setPlaceholder(ctx.t("ai.modal.placeholder"))
                .setMinLength(1)
                .setMaxLength(AiMessageModal.MESSAGE_MAX_LENGTH)
                .build()

        val modal =
            Modal
                .create(AiMessageModal.MODAL_ID, ctx.t("ai.modal.title"))
                .addComponents(Label.of(ctx.t("ai.modal.label"), messageInput))
                .build()

        event.replyModal(modal).await()
    }
}
