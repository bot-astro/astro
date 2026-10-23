package space.astro.bot.commands.vc.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCPrivateChatManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Containers
import space.astro.shared.core.repositories.redis.TemporaryVCRepository
import space.astro.bot.interactions.handlers.ButtonHandler

@Component
class ChatButton(
    contextResolverFactory: VcContextResolverFactory,
    private val temporaryVCRepository: TemporaryVCRepository,
    private val vcPrivateChatManager: VCPrivateChatManager,
) : ButtonHandler<VcInteractionContext> {
    override val id = "button/vc/chat"
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
        val existingChat = vcCtx.privateChat

        if (existingChat != null) {
            ctx.responder.error("A private text chat already exists: ${existingChat.asMention}")
            return
        }

        val privateTextChat = vcPrivateChatManager.create(ctx.member, vcCtx.generatorSettings, vcCtx.temporaryVC)
        val entity = vcCtx.temporaryVCEntity
        entity.chatID = privateTextChat.id
        entity.chatNameChanges = 0
        entity.lastChatNameChange = null
        entity.chatLogs = false
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, entity) }

        ctx.responder.reply(Containers.default("Created a private text chat: ${privateTextChat.asMention}"))
    }
}
