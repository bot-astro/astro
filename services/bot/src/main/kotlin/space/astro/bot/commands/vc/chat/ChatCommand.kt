package space.astro.bot.commands.vc.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.springframework.stereotype.Component
import space.astro.bot.components.managers.vc.VCPrivateChatManager
import space.astro.bot.interactions.Ack
import space.astro.bot.interactions.InteractionSettings
import space.astro.bot.interactions.context.VcContextResolverFactory
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.bot.ui.Embeds
import space.astro.shared.core.repositories.redis.TemporaryVCRepository
import space.astro.bot.interactions.handlers.SlashHandler

@Component
class ChatCommand(
    contextResolverFactory: VcContextResolverFactory,
    private val temporaryVCRepository: TemporaryVCRepository,
    private val vcPrivateChatManager: VCPrivateChatManager,
) : SlashHandler<VcInteractionContext> {
    companion object {
        private const val CREATE = "create"
        private const val DELETE = "delete"
        private const val LOGS = "logs"
    }

    override val data =
        Commands
            .slash("chat", "Manage the private text chat of your VC")
            .setContexts(InteractionContextType.GUILD)
            .addSubcommands(
                SubcommandData(CREATE, "Create a private text chat for your VC"),
                SubcommandData(DELETE, "Delete the private text chat of your VC"),
                SubcommandData(LOGS, "Toggle logs for your VC text chat"),
            )
    override val settings = InteractionSettings(ack = Ack.DEFER_REPLY_EPHEMERAL)
    override val contextResolver =
        contextResolverFactory.create(
            ownershipRequired = true,
            origin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE,
        )

    override suspend fun handle(
        event: SlashCommandInteractionEvent,
        ctx: VcInteractionContext,
    ) {
        when (event.subcommandName) {
            CREATE -> create(ctx)
            DELETE -> delete(ctx)
            LOGS -> logs(ctx)
        }
    }

    private suspend fun create(ctx: VcInteractionContext) {
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

        ctx.responder.reply(Embeds.default("Created a private text chat: ${privateTextChat.asMention}"))
    }

    private suspend fun delete(ctx: VcInteractionContext) {
        val vcCtx = ctx.vcOperationCTX
        val entity = vcCtx.temporaryVCEntity

        vcCtx.privateChat
            ?.delete()
            ?.reason("delete private text chat from 'chat delete' command")
            ?.queue()
        entity.chatID = null
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, entity) }

        ctx.responder.reply(Embeds.default("Private text chat deleted."))
    }

    private suspend fun logs(ctx: VcInteractionContext) {
        val entity = ctx.vcOperationCTX.temporaryVCEntity
        entity.chatLogs = !entity.chatLogs
        withContext(Dispatchers.IO) { temporaryVCRepository.save(ctx.guildId, entity) }

        ctx.responder.reply(Embeds.default("Logs are now ${if (entity.chatLogs) "enabled" else "disabled"}"))
    }
}
