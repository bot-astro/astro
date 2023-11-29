package space.astro.bot.command.impl.vc.permissions

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.command.*
import space.astro.bot.components.managers.vc.VcPermissionManager
import space.astro.bot.core.ui.Embeds
import space.astro.bot.core.ui.Emojis
import space.astro.bot.models.discord.vc.VCOperationCTX
import space.astro.shared.core.daos.TemporaryVCDao
import space.astro.shared.core.models.database.VCState

@Command(
    name = "unhide",
    description = "Un-hide your voice channel making it visible to everyone",
    category = CommandCategory.VC
)
class UnhideCommand(
    val vcPermissionManager: VcPermissionManager,
    val temporaryVCDao: TemporaryVCDao
) : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcCommandContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcCommandContext,
    ) {
        vcPermissionManager.changeState(ctx.vcOperationCTX, VCState.UNHIDDEN)
        ctx.vcOperationCTX.queueUpdatedManagers()
        temporaryVCDao.save(ctx.guildId, ctx.vcOperationCTX.temporaryVCData)

        event.replyEmbeds(
            Embeds.default(
            "Your VC has been ${Emojis.unhide.formatted} un-hidden!"
        )).setEphemeral(true).queue()
    }
}