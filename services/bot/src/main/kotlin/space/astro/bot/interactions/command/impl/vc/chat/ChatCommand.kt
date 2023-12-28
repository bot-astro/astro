package space.astro.bot.interactions.command.impl.vc.chat

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.components.managers.vc.VCPrivateChatManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.util.extention.asEnabledOrDisabled

@Command(
    name = "chat",
    description = "Manage the private text chat of your VC",
    category = CommandCategory.VC,
    premium = true
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
            event.replyEmbeds(Embeds.error("A private text chat already exists: ${ctx.vcOperationCTX.privateChat.asMention}"))
                .setEphemeral(true)
                .queue()

            return
        }

        event.deferReply(true).await()

         val privateTextChat = vcPrivateChatManager.create(ctx.member, ctx.vcOperationCTX.generatorData, ctx.vcOperationCTX.temporaryVC)
        ctx.vcOperationCTX.temporaryVCData.chatID = privateTextChat.id
        ctx.vcOperationCTX.temporaryVCData.chatNameChanges = 0
        ctx.vcOperationCTX.temporaryVCData.lastChatNameChange = null
        ctx.vcOperationCTX.temporaryVCData.chatLogs = false
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        event.hook.editOriginalEmbeds(Embeds.default("Created a private text chat: ${privateTextChat.asMention}"))
            .queue()
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
        ctx.vcOperationCTX.privateChat?.delete()?.queue()
        ctx.vcOperationCTX.temporaryVCData.chatID = null
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        event.replyEmbeds(Embeds.default("Private text chat deleted."))
            .setEphemeral(true)
            .queue()
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

        event.replyEmbeds(Embeds.default("Logs are now ${ctx.vcOperationCTX.temporaryVCData.chatLogs.asEnabledOrDisabled()}"))
            .setEphemeral(true)
            .queue()
    }
}