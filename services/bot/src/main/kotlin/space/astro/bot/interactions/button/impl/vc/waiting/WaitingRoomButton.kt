package space.astro.bot.interactions.button.impl.vc.waiting

import dev.minn.jda.ktx.coroutines.await
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import space.astro.bot.components.managers.vc.VCWaitingRoomManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.interactions.InteractionAction
import space.astro.bot.interactions.InteractionIds
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.interactions.button.AbstractButton
import space.astro.bot.interactions.button.Button
import space.astro.bot.interactions.button.ButtonRunnable
import space.astro.bot.interactions.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao

@Button(
    id = InteractionIds.Button.VC_WAITING_ROOM,
    action = InteractionAction.VC_WAITING_ROOM
)
class WaitingRoomButton(
    private val temporaryVCDao: TemporaryVCDao,
    private val vcWaitingRoomManager: VCWaitingRoomManager,
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
        if (ctx.vcOperationCTX.waitingRoom != null) {
            event.replyEmbeds(Embeds.error("A waiting room already exists: ${ctx.vcOperationCTX.waitingRoom.asMention}"))
                .setEphemeral(true)
                .queue()

            return
        }

        event.deferReply(true).await()

        val waitingRoom = vcWaitingRoomManager.create(ctx.member, ctx.vcOperationCTX.generatorData, ctx.vcOperationCTX.temporaryVC, ctx.vcOperationCTX.temporaryVCData.incrementalPosition)
        ctx.vcOperationCTX.temporaryVCData.waitingID = waitingRoom.id
        ctx.vcOperationCTX.temporaryVCData.waitingNameChanges = 0
        ctx.vcOperationCTX.temporaryVCData.lastWaitingNameChange = null
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        event.hook.editOriginalEmbeds(Embeds.default("Created a waiting room: ${waitingRoom.asMention}"))
            .queue()
    }
}