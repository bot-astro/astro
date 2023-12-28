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
    name = "unlock",
    description = "Unlock your voice channel allowing everyone to join it",
    category = CommandCategory.VC
)
class UnlockCommand(
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
        vcPermissionManager.changeState(ctx.vcOperationCTX, VCState.UNLOCKED)
        ctx.vcOperationCTX.queueUpdatedManagers()
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        event.replyEmbeds(
            Embeds.default(
            "Your VC has been ${Emojis.unlock.formatted} unlocked!"
        )).setEphemeral(true).queue()
    }
}