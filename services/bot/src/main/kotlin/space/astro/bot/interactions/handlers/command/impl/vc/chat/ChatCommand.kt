package space.astro.bot.interactions.handlers.command.impl.vc.chat

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.components.managers.vc.VCPrivateChatManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.command.AbstractCommand
import space.astro.bot.interactions.handlers.command.Command
import space.astro.bot.interactions.handlers.command.CommandCategory
import space.astro.bot.interactions.handlers.command.SubCommand
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.util.extention.asEnabledOrDisabled

@Command(
    name = "chat",
    description = "Manage the private text chat of your VC",
    category = CommandCategory.VC,
    action = InteractionAction.VC_CHAT
)
class ChatCommand(
    private val temporaryVCDao: TemporaryVCDao,
    private val vcPrivateChatManager: VCPrivateChatManager,
) : AbstractCommand() {
    @SubCommand(
        name = "create",
        description = "Create a private text chat for your VC"
    )
    suspend fun create(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        if (ctx.vcOperationCTX.privateChat != null) {
            ctx.replyHandler.replyEmbed(Embeds.error("A private text chat already exists: ${ctx.vcOperationCTX.privateChat.asMention}"))
            return
        }

        ctx.replyHandler.deferReply()

        val privateTextChat = vcPrivateChatManager.create(ctx.member, ctx.vcOperationCTX.generatorData, ctx.vcOperationCTX.temporaryVC)
        ctx.vcOperationCTX.temporaryVCData.chatID = privateTextChat.id
        ctx.vcOperationCTX.temporaryVCData.chatNameChanges = 0
        ctx.vcOperationCTX.temporaryVCData.lastChatNameChange = null
        ctx.vcOperationCTX.temporaryVCData.chatLogs = false
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        ctx.replyHandler.replyEmbed(Embeds.default("Created a private text chat: ${privateTextChat.asMention}"))
    }

    @SubCommand(
        name = "delete",
        description = "Delete the private text chat of your VC"
    )
    suspend fun delete(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        ctx.replyHandler.deferReply()

        ctx.vcOperationCTX.privateChat?.delete()?.queue()
        ctx.vcOperationCTX.temporaryVCData.chatID = null
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        ctx.replyHandler.replyEmbed(Embeds.default("Private text chat deleted."))
    }

    @SubCommand(
        name = "logs",
        description = "Toggle logs for your VC text chat"
    )
    suspend fun logs(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        ctx.vcOperationCTX.temporaryVCData.chatLogs = !ctx.vcOperationCTX.temporaryVCData.chatLogs
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        ctx.replyHandler.replyEmbed(Embeds.default("Logs are now ${ctx.vcOperationCTX.temporaryVCData.chatLogs.asEnabledOrDisabled()}"))
    }
}