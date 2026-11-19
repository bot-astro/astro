package space.astro.bot.commands.ai

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import space.astro.bot.components.ai.AiApplyOutcome
import space.astro.bot.components.ai.AiAssistantClient
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.BaseContextResolver
import space.astro.bot.interactions.context.InteractionContext
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.ui.AiMessages

private val log = KotlinLogging.logger { }

/**
 * Applies the proposal whose id follows the `?` in the component id
 */
@Component
class AiApproveButton(
    private val client: AiAssistantClient,
) : ButtonHandler<InteractionContext> {
    companion object {
        const val ID = "button/ai/approve"
    }

    override val id = ID
    override val settings =
        InteractionSettings(
            ack = Ack.DEFER_EDIT,
            memberPermissions = setOf(Permission.MANAGE_SERVER),
        )
    override val contextResolver = BaseContextResolver

    override suspend fun handle(
        event: ButtonInteractionEvent,
        ctx: InteractionContext,
    ) {
        val proposalId = event.componentId.substringAfter('?')

        val outcome =
            try {
                withContext(Dispatchers.IO) { client.apply(ctx.guildId, proposalId, ctx.memberId) }
            } catch (e: RestClientException) {
                log.warn(e) { "AI proposal apply failed in guild ${ctx.guildId}" }
                ctx.responder.error("ai.error.unavailable")
                return
            }

        val status =
            when (outcome) {
                AiApplyOutcome.APPLIED -> ctx.t("ai.proposal.applied", "user" to ctx.member.effectiveName)
                AiApplyOutcome.CONFLICT -> ctx.t("ai.proposal.stale")
                AiApplyOutcome.FORBIDDEN -> {
                    ctx.responder.error("ai.error.forbidden")
                    return
                }
            }

        ctx.responder.reply(*AiMessages.resolveProposal(event.message.components, event.componentId, status).toTypedArray())
    }
}
