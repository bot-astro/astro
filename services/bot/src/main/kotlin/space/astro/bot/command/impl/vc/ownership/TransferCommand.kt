package space.astro.bot.command.impl.vc.ownership

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import space.astro.bot.command.*
import space.astro.bot.models.discord.vc.VCOperationCTX

@Command(
    name = "ban",
    description = "Bans someone from joining your voice channel",
    category = CommandCategory.VC
)
class TransferCommand : AbstractCommand() {
    @BaseCommand
    suspend fun run(
        event: SlashCommandInteractionEvent,
        @VcCommandContextInfo(
            ownershipRequired = true,
            vcOperationOrigin = VCOperationCTX.VCOperationOrigin.STATE_CHANGE
        )
        ctx: VcCommandContext,
    ) {

    }
}