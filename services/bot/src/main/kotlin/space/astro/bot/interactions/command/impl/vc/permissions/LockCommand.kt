package space.astro.bot.interactions.command.impl.vc.permissions

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.interactions.command.*
import space.astro.bot.components.managers.vc.VCPermissionManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.interactions.VcInteractionContext
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.models.database.VCState

@Command(
    name = "lock",
    description = "Lock your channel so that no one can join it",
    category = CommandCategory.VC
)
class LockCommand(
    val vcPermissionManager: VCPermissionManager,
    val temporaryVCDao: TemporaryVCDao
) : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcInteractionContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcInteractionContext,
    ) {
        vcPermissionManager.changeState(ctx.vcOperationCTX, VCState.LOCKED)
        ctx.vcOperationCTX.queueUpdatedManagers()
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        event.replyEmbeds(Embeds.default(
            "Your VC has been ${Emojis.lock.formatted} locked!"
        )).setEphemeral(true).queue()
    }
}