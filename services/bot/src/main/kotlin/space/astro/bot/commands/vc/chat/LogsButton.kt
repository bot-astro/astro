package space.astro.bot.commands.vc.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.handlers.ButtonHandler
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers
import space.astro.shared.core.repositories.redis.TemporaryVCRepository

@Component
class LogsButton(
    contextResolverFactory: VcContextResolverFactory,
    private val temporaryVCRepository: TemporaryVCRepository,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/logs"
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: ButtonInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        val entity = ctx.vcOperationCTX.temporaryVCEntity
        entity.chatLogs = !entity.chatLogs
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, entity) }

        ctx.responder.reply(Containers.default(ctx.t(if (entity.chatLogs) "vc.chat.logs_enabled" else "vc.chat.logs_disabled")))
    }
}
