package space.astro.bot.commands.ai

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.components.ai.AiAssistantManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.BaseContextResolver
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.ModalHandler

@Component
class AiMessageModal(
    private val aiAssistantManager: AiAssistantManager,
) : ModalHandler<InteractionContext> {
    companion object {
        const val MODAL_ID = "modal/ai/message"
        const val MESSAGE_INPUT_ID = "message"
        const val MESSAGE_MAX_LENGTH = 1000
    }

    override val id = MODAL_ID

    // visible to the whole thread, the deferred reply shows the thinking state while the assistant works
    override val settings =
        InteractionSettings(
            ack = Ack.DEFER_REPLY,
            memberPermissions = setOf(Permission.MANAGE_SERVER),
        )
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: ModalInteractionEvent,
        ctx: InteractionContext,
    ) {
        val text = event.getValue(MESSAGE_INPUT_ID)?.asString?.trim()

        if (text.isNullOrBlank()) {
            ctx.responder.error("ai.message.invalid")
            return
        }

        val reply =
            aiAssistantManager.respond(
                ctx.guildId,
                ctx.memberId,
                event.channelIdLong.toString(),
                text,
                ctx.guildSettings,
                quote = text,
            )

        ctx.responder.reply(*reply.components.toTypedArray(), ephemeral = false)
    }
}
