package space.astro.bot.interactions.handlers.button.impl.vc.chat

import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import space.astro.bot.components.managers.vc.VCPrivateChatManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.context.VcInteractionContext
import space.astro.bot.interactions.context.VcInteractionContextInfo
import space.astro.bot.interactions.handlers.button.Button
import space.astro.bot.interactions.handlers.button.ButtonRunnable
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao

@Button(
    id = "chat",
    action = InteractionAction.VC_CHAT
)
class ChatButton(
    private val temporaryVCDao: TemporaryVCDao,
    private val vcPrivateChatManager: VCPrivateChatManager,
) : space.astro.bot.interactions.handlers.button.AbstractButton() {
    @ButtonRunnable
    suspend fun run(
        event: ButtonInteractionEvent,
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
}