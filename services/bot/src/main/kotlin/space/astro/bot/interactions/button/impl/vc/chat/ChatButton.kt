package space.astro.bot.interactions.button.impl.vc.chat

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import space.astro.bot.components.managers.vc.VCPrivateChatManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.button.AbstractButton
import space.astro.bot.interactions.button.Button
import space.astro.bot.interactions.button.ButtonRunnable
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao

@Button(
    id = "chat",
    action = InteractionAction.VC_CHAT
)
class ChatButton(
    private val temporaryVCDao: TemporaryVCDao,
    private val vcPrivateChatManager: VCPrivateChatManager,
) : AbstractButton() {
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
}