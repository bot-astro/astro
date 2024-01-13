package space.astro.bot.interactions.handlers.command.impl.vc.waiting

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.components.managers.vc.VCWaitingRoomManager
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

@Command(
    name = "waiting-room",
    description = "Manage the waiting room of your VC",
    category = CommandCategory.VC,
    action = InteractionAction.VC_WAITING_ROOM
)
class WaitingRoomCommand(
    private val temporaryVCDao: TemporaryVCDao,
    private val vcWaitingRoomManager: VCWaitingRoomManager,
) : AbstractCommand() {
    @SubCommand(
        name = "create",
        description = "Create a waiting room for your VC"
    )
    suspend fun create(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        if (ctx.vcOperationCTX.waitingRoom != null) {
            ctx.replyHandler.replyEmbed(Embeds.error("A waiting room already exists: ${ctx.vcOperationCTX.waitingRoom.asMention}"))
            return
        }

        ctx.replyHandler.deferReply()

        val waitingRoom = vcWaitingRoomManager.create(ctx.member, ctx.vcOperationCTX.generatorData, ctx.vcOperationCTX.temporaryVC, ctx.vcOperationCTX.temporaryVCData.incrementalPosition)
        ctx.vcOperationCTX.temporaryVCData.waitingID = waitingRoom.id
        ctx.vcOperationCTX.temporaryVCData.waitingNameChanges = 0
        ctx.vcOperationCTX.temporaryVCData.lastWaitingNameChange = null
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        ctx.replyHandler.replyEmbed(Embeds.default("Created a waiting room: ${waitingRoom.asMention}"))
    }

    @SubCommand(
        name = "delete",
        description = "Delete the waiting room of your VC"
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

        ctx.vcOperationCTX.waitingRoom?.delete()?.queue()
        ctx.vcOperationCTX.temporaryVCData.waitingID = null
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        ctx.replyHandler.replyEmbed(Embeds.default("Waiting room deleted."))
    }
}