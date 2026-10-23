package space.astro.bot.commands.vc.waiting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCWaitingRoomManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Embeds
import space.astro.shared.core.repositories.redis.TemporaryVCRepository
import space.astro.bot.interactions.handlers.ButtonHandler

@Component
class WaitingRoomButton(
    contextResolverFactory: VcContextResolverFactory,
    private val temporaryVCRepository: TemporaryVCRepository,
    private val vcWaitingRoomManager: VCWaitingRoomManager,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/waiting-room"
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
        val vcCtx = ctx.vcOperationCTX
        val existingWaitingRoom = vcCtx.waitingRoom

        if (existingWaitingRoom != null) {
            ctx.responder.error("A waiting room already exists: ${existingWaitingRoom.asMention}")
            return
        }

        val entity = vcCtx.temporaryVCEntity
        val waitingRoom =
            vcWaitingRoomManager.create(ctx.member, vcCtx.generatorSettings, vcCtx.temporaryVC, entity.incrementalPosition)
        entity.waitingID = waitingRoom.id
        entity.waitingNameChanges = 0
        entity.lastWaitingNameChange = null
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, entity) }

        ctx.responder.reply(Embeds.default("Created a waiting room: ${waitingRoom.asMention}"))
    }
}
